# Kỳ Môn Binh Pháp — ứng dụng Android

Ứng dụng Android hiển thị trang «Kỳ Môn Binh Pháp» (奇門兵法) trong một WebView toàn màn hình.
Trang, mã JavaScript và phông chữ đều nằm sẵn trong APK nên ứng dụng chạy hoàn toàn ngoại tuyến,
không xin quyền truy cập Internet. Nút ở đầu trang (cạnh «Chọn giờ») chọn giao diện: theo máy,
sáng hoặc tối, cùng bảng màu với artifact; thanh trạng thái của Android đổi màu theo.

## Tải và cài đặt

1. Trên điện thoại, tải bản mới nhất:
   **https://github.com/nguyengiathong/KyMonBinhPhap/releases/download/apk/KyMonBinhPhap.apk**
   (hoặc vào mục *Releases* của repo).
2. Mở file vừa tải. Nếu Android hỏi, cho phép trình duyệt hoặc trình quản lý tệp
   «Cài đặt ứng dụng không rõ nguồn gốc».
3. Cần Android 7.0 trở lên.

Mỗi lần push, GitHub Actions (workflow *Build APK*) tự dựng APK. Bản dựng từ nhánh mặc định
thay thế release `apk` ở trên; bản dựng của mọi nhánh còn nằm ở mục *Artifacts* của từng lần chạy.

## Cập nhật nội dung trang

Khi có bản HTML mới (ví dụ tải từ Claude), chạy:

```bash
python3 tools/prepare_web.py duong/dan/Ky-Mon-Binh-Phap.html
```

Script ghi `app/src/main/assets/www/index.html`, bổ sung phần khung tài liệu (`<!doctype>`,
`viewport`), tải các phông Google Fonts mà trang dùng về `assets/www/fonts/` để chạy offline và
gắn `app.css` / `app.js` vào trang. Hai file này là phần riêng của app (thanh điều khiển xếp
ba dòng, nút sáng/tối), không bị ghi đè khi tạo lại trang. Commit rồi push là có APK mới.

## Tự dựng APK trên máy

Cần JDK 17+ và Android SDK (cài sẵn khi có Android Studio):

```bash
./gradlew assembleRelease
```

APK nằm ở `app/build/outputs/apk/release/app-release.apk`.

## Khóa ký ứng dụng

APK được ký bằng khóa debug đi kèm repo (`app/debug.keystore`, mật khẩu `android`) để bản sau
cài đè được lên bản trước. Khóa này công khai: đủ cho dùng cá nhân, nhưng không nên dùng để phát hành
lên Google Play. Muốn ký bằng khóa riêng, thêm bốn secret vào repo
(*Settings → Secrets and variables → Actions*):

| Secret | Giá trị |
| --- | --- |
| `KMBP_KEYSTORE_BASE64` | file keystore, mã hóa base64 (`base64 -w0 release.jks`) |
| `KMBP_KEYSTORE_PASSWORD` | mật khẩu keystore |
| `KMBP_KEY_ALIAS` | alias của khóa |
| `KMBP_KEY_PASSWORD` | mật khẩu khóa |

Đổi khóa ký thì phải gỡ bản cũ trên máy trước khi cài bản mới.

## Cấu trúc

- `app/src/main/assets/www/` — trang web và phông chữ (Be Vietnam Pro, Newsreader; giấy phép SIL OFL 1.1, kèm theo trong thư mục `fonts/`)
- `app/src/main/assets/www/app.css`, `app.js` — phần riêng của app: bố cục thanh điều khiển, chín ô bàn cửu cung luôn bằng nhau, chọn giao diện sáng/tối
- `app/src/main/java/io/github/nguyengiathong/kymonbinhphap/MainActivity.java` — WebView, nút Back đi lùi qua các thẻ của trang, màu thanh hệ thống theo giao diện
- `app/src/main/res/` — giao diện hệ thống, biểu tượng ấn «兵»
- `tools/prepare_web.py` — chuyển file HTML thành gói web ngoại tuyến
- `.github/workflows/build-apk.yml` — dựng và phát hành APK
