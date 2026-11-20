# VocaBubble Android — Trình Đọc Màn Hình Nổi & Karaoke Tiếng Việt

Dự án Android Native (Kotlin) được xuất trực tiếp từ **VocaBubble** với đầy đủ cấu hình màu sắc, cỡ chữ và tốc độ đọc hiện tại của bạn.

## Tính năng tích hợp sẵn
1. **Menu Copy / Cut / Paste toàn hệ thống (`ACTION_PROCESS_TEXT`)**: Bôi đen bất kỳ đoạn văn bản nào trên Facebook, Zalo, Chrome, Messenger, PDF... -> chọn **"Đọc với VocaBubble"**.
2. **Bubble nổi kéo thả hít mép (`SYSTEM_ALERT_WINDOW`)**:
   - Chế độ Mở rộng: Hiển thị văn bản bôi đen với hiệu ứng chạy sáng từng từ karaoke + tự động cuộn + nút gạt **Chạm từ để nhảy** chống cấn máy.
   - Chế độ Thu gọn (Mini): Chỉ còn 1 nút tròn Dừng/Phát kéo thả tự do và tự hít vào mép màn hình.
   - Chỉnh trực tiếp Cỡ chữ (17sp), Tốc độ (1.0x) và Màu nền/Chữ/Karaoke ngay trong Bubble.
3. **Phát nền & Màn hình khóa (`ForegroundService` + `MediaSessionCompat`)**: Đọc đoạn dài hàng chục nghìn chữ không lag nhờ chia câu Tiếng Việt, điều khiển Phát/Dừng ngay trên thanh thông báo & màn hình khóa, nhớ vị trí đọc dở.

## Cách tạo file `.apk` nhanh nhất (Không cần cài Android Studio)
1. Tạo 1 repository mới trên GitHub và tải toàn bộ thư mục này lên.
2. Vào tab **Actions** -> workflow **Build VocaBubble APK** sẽ tự chạy trong ~2-3 phút.
3. Tải file **VocaBubble-debug-apk** trong mục **Artifacts**, gửi sang điện thoại Android và bấm cài đặt!

## Hoặc build trực tiếp bằng Android Studio
1. Mở thư mục này bằng **Android Studio (Ladybug / Meerkat trở lên)**.
2. Chọn **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.
3. Cài đặt lên điện thoại, mở ứng dụng **VocaBubble** và cấp 2 quyền: **Hiển thị trên ứng dụng khác** & **Thông báo**.
