clean:
	rm -rf apk/

apk: clean
	./gradlew --no-daemon app:assembleDebug
	mkdir -p apk
	cp ./app/build/outputs/apk/debug/app-debug.apk apk/
	echo "\n* Android APK file available in apk/app-debug.apk"

prepare_smobf:
	@if [ ! -d "apk/app-debug" ]; then \
		apktool d apk/app-debug.apk; \
	else \
		echo "* apktool NOT called, REUSE!"; \
	fi
	@echo "==================================="
	@echo "Available Test Functions:"
	@echo "==================================="
	@rm -rf smobf_dir
	@mkdir -p smobf_dir
	@target_file=$$(find apk/app-debug -iname k.smali); \
	cp $$target_file smobf_dir/; \
	echo "a()Ljava/lang/String;" > smobf_dir/target_functions.txt; \
	cat smobf_dir/target_functions.txt
	@echo "==================================="
	@echo "* smobf files in smobf_dir/"

obf_apk: prepare_smobf
	rm -rf $(PWD)/smobf_out
	docker run -t \
		-v $(PWD)/smobf_dir:/workspace/input_dir \
		-v $(PWD)/smobf_out:/workspace/output_dir \
		-e INPUT_DIR=/workspace/input_dir \
		-e OUTPUT_DIR=/workspace/output_dir \
		castle-android-obfpip

package_obf_apk: obf_apk
	@echo "==================================="
	@echo "Packaging obfuscated files back into APK"
	@echo "==================================="
	@cp smobf_out/libobf_arm64.so apk/app-debug/lib/arm64-v8a/libsmobftest.so
	@echo "* Native library copied to: apk/app-debug/lib/arm64-v8a/libobf_arm64.so"
	@echo "* NOTE: Functions.smali NOT copied back in testing mode"
	@echo "==================================="
	@echo "Packages and Sign apk"
	apktool b apk/app-debug
	signApk apk/app-debug/dist/app-debug