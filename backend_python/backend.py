#!/usr/bin/env python3
import logging
import json
import jwt
import os

from datetime import datetime, UTC

from castle.configuration import configuration
from castle.client import Client, ContextPrepare
from castle.errors import CastleError, InvalidRequestTokenError

from dotenv import load_dotenv

load_dotenv()

if "CASTLE_API_SECRET" not in os.environ:
    raise EnvironmentError("Missing 'CASTLE_API_SECRET' environment variable")
configuration.api_secret = os.environ["CASTLE_API_SECRET"]

from flask import Flask, request, jsonify  # noqa: E402

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
castle_client = Client()


@app.route("/check/", methods=["POST"])
def check_inputpin():
    input_pin = None
    castle_token = None
    if request.is_json:
        data = request.get_json()
        input_pin = data.get("pin", None)
        castle_token = data.get("castle_token", None)

    if not input_pin:  # or not castle_token:
        return jsonify({"valid": False, "message": "No no no no :)"}), 400

    try:
        # not so sure here,
        # forge static ip from office

        office_ip = "78.138.21.203"
        req_context = ContextPrepare.call(request)
        req_context["ip"] = office_ip
        # req_context['X-Castle-Request-Token'] = castle_token # wtf ???

        result = castle_client.filter(
            {
                "request_token": castle_token,
                "context": req_context,
                "type": "$registration",
                "status": "$attempted",
                "params": {"email": os.environ.get("USER_EMAIL", "default@castle.io")},
            }
        )

        if result["policy"]["action"] == "deny":
            return jsonify({"valid": False, "message": "Oh hell no :o"}), 400
    except InvalidRequestTokenError as e:
        logger.error(f"InvalidRequestTokenError: {str(e).replace('\n', '')}")
        # Invalid request token is very likely a bad actor bypassing fingerprinting
        return jsonify({"valid": False, "message": "Try again :("}), 401
    except CastleError as e:
        # Allow the attempt - most likely a server or timeout error
        logger.error(f"CastleError: {str(e)}")

    token_info = {
        "valid": True,
        "message": json.dumps(
            {
                "received_at": datetime.now(UTC).isoformat() + "Z",
                "client_ip": request.remote_addr,
                "user_agent": request.headers.get("User-Agent", "Unknown"),
            }
        ),
    }
    logger.info(f"Received Info: {token_info}")

    return jsonify(token_info), 200


@app.route("/castle_user_jwt/", methods=["GET"])
def serve_castle_jwt():
    payload = {
        "id": os.environ.get("USER_ID", "DEFAULT_ID"),
        "email": os.environ.get("USER_EMAIL", "default@castle.io"),
        "phone": "+1415232183",
    }

    token = jwt.encode(payload, os.environ["CASTLE_API_SECRET"], "HS256")
    return jsonify({"token": token})


@app.route("/health", methods=["GET"])
def health_check():
    return jsonify(
        {
            "status": "healthy",
            "service": "input-verification-server",
            "timestamp": datetime.now(UTC).isoformat() + "Z",
        }
    ), 200


@app.errorhandler(404)
def not_found(error):
    logger.error(error)
    return jsonify(
        {
            "success": False,
            "error": "Not found",
            "message": "Only POST /check and GET /health endpoints are available",
        }
    ), 404


@app.errorhandler(405)
def method_not_allowed(error):
    logger.error(error)
    return jsonify(
        {
            "success": False,
            "error": "Method not allowed",
            "message": "Use POST for /check endpoint",
        }
    ), 405


if __name__ == "__main__":
    logger.info("Starting Pin Verification Server on port 9988...")
    app.run(host="0.0.0.0", port=9988, debug=True)
