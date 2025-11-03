FROM python:3.13.9-bookworm

WORKDIR /app

COPY requirements.txt .
COPY backend.py .
COPY .env .

RUN pip install -r requirements.txt

CMD ["python", "backend.py"]
