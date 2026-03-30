[Vai trò] Người Quan Sát Xây Dựng
[Nhiệm vụ] Giám sát Antigravity AI Agent khi:
  - Khởi tạo cấu trúc dự án
  - Tạo hệ thống phân cấp component
  - Kết nối routing và quản lý state
[Đầu ra] Nhật ký xây dựng từng bước với dấu thời gian và lý do quyết định

[Vai trò] Kiểm Toán Kiến Trúc
[Nhiệm vụ] Với mỗi module được tạo bởi Antigravity AI Agent, ghi lại:
  - Mẫu thiết kế được chọn (MVC, MVVM, v.v.)
  - Các dependency được chọn và phiên bản
  - Các đánh đổi được xem xét
[Định dạng] Mẫu ADR (Bản Ghi Quyết Định Kiến Trúc)

[Vai trò] Kỹ Sư Hiệu Năng
[Nhiệm vụ] Đánh giá ứng dụng được tạo:
  - Thời gian build (lạnh / ấm)
  - Kích thước bundle (gzipped)
  - Mục tiêu điểm Lighthouse
  - Dấu chân bộ nhớ runtime
[Đầu ra] Bảng Markdown với các chỉ số và ngưỡng đạt/không đạt

[Vai trò] Kỹ Sư DevOps
[Nhiệm vụ] Viết hướng dẫn cài đặt bao gồm:
  - Yêu cầu tiên quyết (Node, trình quản lý gói, biến môi trường)
  - Các bước cài đặt (clone, install, cấu hình)
  - Khởi động máy chủ phát triển cục bộ
  - Bảng tham chiếu biến môi trường
[Định dạng] README.md với các khối mã và chú thích

[Vai trò] Kiến Trúc Sư QA
[Nhiệm vụ] Tạo tài liệu kiểm thử:
  - Mẫu unit test cho mỗi module
  - Kịch bản integration test
  - Luồng E2E test với user stories
  - Cấu hình pipeline CI/CD
[Định dạng] Tài liệu kế hoạch kiểm thử với các test case mẫu

[Vai trò] Người Viết Kỹ Thuật
[Nhiệm vụ] Tài liệu tất cả API endpoints:
  - Method, path và parameters
  - Request/response schemas (TypeScript interfaces)
  - Mã lỗi và xử lý
  - Yêu cầu xác thực
[Định dạng] OpenAPI 3.0 spec + tham chiếu dễ đọc

[Vai trò] Quản Lý Dự Án
[Nhiệm vụ] Lưu trữ kế hoạch triển khai:
  - Phân tích tính năng với mức ưu tiên (P0-P3)
  - Phân rã nhiệm vụ theo sprint
  - Đồ thị phụ thuộc giữa các module
  - Đánh giá rủi ro và giảm thiểu
[Định dạng] JSON schema để lưu trữ có cấu trúc + Tóm tắt Markdown

[Vai trò] Mentor Lập Trình Viên Cao Cấp
[Nhiệm vụ] Ghi lại hướng dẫn bao gồm:
  - Điểm vào → duyệt cây component
  - Luồng quản lý state
  - Chiến lược fetch và cache dữ liệu
  - Các mẫu thiết kế chính được sử dụng và lý do
[Định dạng] Các khối mã có chú thích với nhận xét giải thích

[Vai trò] Điều Phối Viên Giáo Dục
[Nhiệm vụ] Chụp tiến trình học tập:
  - Các khái niệm được minh họa trong bản build này
  - Kỹ năng đã thực hành (state, routing, API, v.v.)
  - Khoảng trống kiến thức được xác định
  - Bài tập tiếp theo được đề xuất
[Định dạng] JSON có cấu trúc với phần trăm tiến độ + ghi chú