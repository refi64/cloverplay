#include <android/log.h>
#include <exception>
#include <functional>
#include <jni.h>
#include <string_view>
#include <type_traits>

#include "service.h"

namespace {

void ThrowRuntimeException(JNIEnv* env, std::string message) {
  jclass jex = env->FindClass("java/lang/RuntimeException");
  env->ThrowNew(jex, message.c_str());
}

void ThrowCxxException(JNIEnv* env, std::string_view prefix, const std::exception& ex) {
  ThrowRuntimeException(env, fmt::format("{}: {}", prefix, ex.what()));
}

void ThrowStatus(JNIEnv* env, std::string_view prefix, absl::Status status) {
  ThrowRuntimeException(env, fmt::format("{}: {}", prefix, status.ToString()));
}

}  // namespace

#define CLOVER_JNI_METHOD(name) Java_com_refi64_cloverplay_CloverServiceJNI_##name

extern "C" {

JNIEXPORT jlong JNICALL CLOVER_JNI_METHOD(create)(JNIEnv* env, jobject self) {
  try {
    StatusOr<Service> service = Service::Create();
    if (!service.ok()) {
      ThrowStatus(env, "Failed to create service", service.TakeStatus());
      return 0;
    }

    return reinterpret_cast<jlong>(new Service(std::move(service.TakeValue())));
  } catch (std::exception& ex) {
    ThrowCxxException(env, "Exception creating service", ex);
    return 0;
  }
}

JNIEXPORT void JNICALL CLOVER_JNI_METHOD(sendEvents)(JNIEnv* env, jobject self, jlong j_service,
                                                     jstring j_req_json) {
  if (j_service == 0) {
    ThrowRuntimeException(env, "Unexpected null service");
    return;
  }

  Service* service = reinterpret_cast<Service*>(j_service);
  const char* req_json = env->GetStringUTFChars(j_req_json, JNI_FALSE);

  try {
    if (absl::Status status = service->HandleRequest(req_json); !status.ok()) {
      ThrowStatus(env, "Error handling events", std::move(status));
    }
  } catch (std::exception& ex) {
    ThrowCxxException(env, "Exception sending events", ex);
  }
}

JNIEXPORT void JNICALL CLOVER_JNI_METHOD(destroy)(JNIEnv* env, jobject self, jlong j_service) {
  Service* service = reinterpret_cast<Service*>(j_service);
  delete service;
}
}
