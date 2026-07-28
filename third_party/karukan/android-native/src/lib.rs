use jni::{
    JNIEnv,
    objects::{JClass, JString},
    sys::{jint, jlong, jobjectArray},
};
use karukan_engine::kanji::{Backend, KanaKanjiConverter};
use std::ptr;

struct AndroidConverter {
    converter: KanaKanjiConverter,
}

fn java_string(env: &mut JNIEnv<'_>, value: JString<'_>) -> Result<String, String> {
    env.get_string(&value)
        .map(|s| s.into())
        .map_err(|error| error.to_string())
}

fn report_error(env: &mut JNIEnv<'_>, message: impl AsRef<str>) {
    let _ = env.throw_new("java/lang/IllegalStateException", message.as_ref());
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_me_zssu_ime_karukan_KarukanNative_nativeCreate(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    model_path: JString<'_>,
    tokenizer_path: JString<'_>,
    threads: jint,
) -> jlong {
    let result = (|| {
        let model_path = java_string(&mut env, model_path)?;
        let tokenizer_path = java_string(&mut env, tokenizer_path)?;
        let backend = Backend::from_files(model_path, tokenizer_path, "jinen-v1-xsmall-q5");
        let mut converter = KanaKanjiConverter::new(backend).map_err(|error| error.to_string())?;
        converter.set_n_threads(threads.max(1) as u32);
        Ok::<_, String>(Box::new(AndroidConverter { converter }))
    })();

    match result {
        Ok(converter) => Box::into_raw(converter) as jlong,
        Err(error) => {
            report_error(&mut env, error);
            0
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_me_zssu_ime_karukan_KarukanNative_nativeConvert(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    handle: jlong,
    reading: JString<'_>,
    context: JString<'_>,
    candidate_count: jint,
) -> jobjectArray {
    if handle == 0 {
        report_error(&mut env, "Karukan converter is not loaded");
        return ptr::null_mut();
    }

    let result = (|| {
        let reading = java_string(&mut env, reading)?;
        let context = java_string(&mut env, context)?;
        // The Kotlin side owns the handle and serialises all calls on one executor.
        let converter = unsafe { &*(handle as *const AndroidConverter) };
        converter
            .converter
            .convert(&reading, &context, candidate_count.clamp(1, 5) as usize)
            .map_err(|error| error.to_string())
    })();

    let candidates = match result {
        Ok(value) => value,
        Err(error) => {
            report_error(&mut env, error);
            return ptr::null_mut();
        }
    };
    let string_class = match env.find_class("java/lang/String") {
        Ok(value) => value,
        Err(error) => {
            report_error(&mut env, error.to_string());
            return ptr::null_mut();
        }
    };
    let array =
        match env.new_object_array(candidates.len() as i32, string_class, JString::default()) {
            Ok(value) => value,
            Err(error) => {
                report_error(&mut env, error.to_string());
                return ptr::null_mut();
            }
        };
    for (index, candidate) in candidates.iter().enumerate() {
        let value = match env.new_string(candidate) {
            Ok(value) => value,
            Err(error) => {
                report_error(&mut env, error.to_string());
                return ptr::null_mut();
            }
        };
        if let Err(error) = env.set_object_array_element(&array, index as i32, value) {
            report_error(&mut env, error.to_string());
            return ptr::null_mut();
        }
    }
    array.into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_me_zssu_ime_karukan_KarukanNative_nativeDestroy(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
    handle: jlong,
) {
    if handle != 0 {
        unsafe {
            drop(Box::from_raw(handle as *mut AndroidConverter));
        }
    }
}
