#include "service.h"

absl::Status Run() {
  Service service = TRY_STATUS_OR(Service::Create());
  for (;;) {
    std::string req_json;
    if (!std::getline(std::cin, req_json)) {
      return absl::Status(absl::StatusCode::kResourceExhausted, "EOF on stdin");
    }

    if (req_json == "q") {
      std::cerr << "Quitting..." << std::endl;
      return absl::OkStatus();
    }

    if (absl::Status status = service.HandleRequest(req_json); !status.ok()) {
      std::cout << status.ToString();
    } else {
      std::cout << "Success";
    }
  }
}

int main() {
  if (absl::Status status = Run(); !status.ok()) {
    CheckStatus(status);
    return 1;
  }

  return 0;
}
