#include "service.h"

absl::Status Run() {
  Service service = TRY_STATUS_OR(Service::Create());
  return service.Run();
}

int main() {
  if (absl::Status status = Run(); !status.ok()) {
    CheckStatus(status);
    return 1;
  }

  return 0;
}
