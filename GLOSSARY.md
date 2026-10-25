# Wedding Event

Ngôn ngữ chung của hai sản phẩm dùng chung một cơ sở dữ liệu: **Lumière Studios admin** (nhân viên studio ảnh cưới) và **WedPlanner** (cặp đôi và nhà cung cấp dịch vụ cưới). Thuật ngữ chuẩn bằng tiếng Anh; định nghĩa bằng tiếng Việt, kèm cách gọi trên giao diện. Khi tên trong code khác thuật ngữ chuẩn, tên trong code được ghi riêng.

## Bán hàng của studio

**Customer** ("Khách hàng"):
Người (thường là một cặp đôi) liên hệ studio, được theo dõi từ lần hỏi đầu tiên đến khi hoàn tất dịch vụ. Vị trí trong pipeline bán hàng là trạng thái của Customer, không phải một khái niệm riêng.
_Tên trong code_: `Lead`
_Avoid_: Lead, khách tiềm năng (khi nói về bản ghi)

**Customer Stage** ("Giai đoạn khách hàng"):
Vị trí của Customer trong pipeline bán hàng: Mới hỏi → Đang tư vấn → Chờ cọc → Đang thực hiện → Đã hoàn thành. Customer ở "Đang thực hiện" khi có ít nhất một Contract đang có hiệu lực, và ở "Đã hoàn thành" khi mọi Contract của họ đều đã là Completed Contract.
_Tên trong code_: `LeadStage`

**Contract** ("Hợp đồng"):
Thoả thuận dịch vụ giữa studio và một Customer cho một Service Package, có tổng giá trị và lịch thanh toán. Có hiệu lực từ khi nhận Deposit. Một Contract gồm nhiều Booking.

**Draft Contract** ("Hợp đồng nháp"):
Contract đã soạn nhưng chưa có hiệu lực vì chưa nhận Deposit.

**Completed Contract** ("Hợp đồng đã hoàn thành"):
Contract đã được thanh toán đủ (Paid in Full) và đã bàn giao xong mọi dịch vụ.
_Avoid_: gọi một Contract là hoàn thành chỉ vì đã trả đủ tiền

**Paid in Full** ("Đã thanh toán đủ"):
Trạng thái thanh toán của một Contract khi Paid Amount bằng tổng giá trị. Đây không phải trạng thái hoàn thành của Contract.

**Cash Entry** ("Phiếu thu/chi"):
Một dòng thu hoặc chi trong sổ quỹ của studio. Khoản chi (ví dụ giặt ủi trang phục, tiền công cộng tác viên) không gắn với Contract nào.
_Tên trong code_: `PaymentTransaction`
_Avoid_: Transaction

**Payment** ("Khoản thanh toán"):
Một Cash Entry loại thu gắn với một Contract.

**Deposit** ("Tiền cọc"):
Đợt thanh toán đầu tiên của một Contract.
_Avoid_: dùng "deposit" cho tổng số tiền đã thu

**Paid Amount** ("Đã thu"):
Tổng mọi khoản đã thu của một Contract, tính cả Deposit.
_Tên trong spec_: `deposit_amount` (đặt tên sai)

**Service Package** ("Gói dịch vụ"):
Một dịch vụ có giá được bán trọn, do studio hoặc một vendor cung cấp. Gói của studio và gói của vendor là cùng một khái niệm, chỉ khác bên cung cấp.
_Tên trong code mobile_: `ServiceItem`, `VendorService`
_Avoid_: Service item, vendor service

**Vendor** ("Nhà cung cấp"):
Doanh nghiệp dịch vụ cưới không phải studio (ví dụ trang trí, nhà hàng, váy cưới) bán Service Package trên WedPlanner.
_Tên trong code_: `VendorProfile`

**Provider** ("Bên cung cấp"):
Bên đứng sau một Service Package: hoặc là studio, hoặc là một Vendor.

## Vận hành studio

**Booking** ("Lịch hẹn"):
Một buổi hẹn của studio với một Customer, có ngày giờ cụ thể và Staff Member được phân công, ví dụ buổi chụp Pre-wedding, buổi thử váy hay lễ cưới. Booking thuộc một dịch vụ đã bán thì thuộc một Contract; Booking tư vấn trước khi ký thì không.
_Avoid_: dùng "booking" cho việc khách đặt dịch vụ của vendor

**Pending Booking** ("Chờ xác nhận"):
Booking đã lên lịch nhưng khách và nhân sự chưa chốt giờ. Các trạng thái tiếp theo là Đã xác nhận, Đang thực hiện, rồi Hoàn thành hoặc Đã huỷ.
_Tên trong code_: `UPCOMING`
_Avoid_: "Sắp tới" (đó là cách lọc theo ngày, không phải trạng thái)

**Staff Member** ("Nhân sự"):
Người làm việc cho studio và được phân công vào Booking, ví dụ thợ chụp, thợ quay hay chuyên viên trang điểm. Có thể không có tài khoản đăng nhập, ví dụ cộng tác viên.
_Avoid_: Staff account, user (khi nói về người làm việc)

**Asset** ("Đồ trong kho"):
Một món đồ của studio để cho thuê hoặc sử dụng: váy cưới, vest hoặc thiết bị chụp.

**Out of Service** ("Ngưng sử dụng"):
Trạng thái nhân viên đặt tay cho một Asset bị hỏng hoặc đang sửa chữa, nên không cho thuê được. Khác với Maintenance Buffer, vốn tự động sau mỗi Rental.
_Tên trong code_: `AssetStatus.MAINTENANCE` ("Bảo trì")

**Rental** ("Thuê đồ"):
Việc giữ một món đồ trong kho (váy, vest, thiết bị) cho một Booking, từ ngày nhận đến ngày trả. Hai Rental của cùng một món đồ không được chồng nhau, kể cả trong Maintenance Buffer.
_Tên trong code_: `AssetReservation`
_Avoid_: Reservation

**Maintenance Buffer** ("Thời gian bảo trì"):
Số ngày một món đồ bị khoá sau khi khách trả để giặt và bảo trì, trước khi cho thuê tiếp.

## WedPlanner

**Couple** ("Cặp đôi"):
Người dùng app WedPlanner lập Wedding Plan cho đám cưới của mình. Đây là khái niệm khác với Customer của studio, dù ngoài đời có thể là cùng một người.
_Avoid_: Customer (khi nói về người dùng app)

**Wedding Plan** ("Kế hoạch cưới"):
Kế hoạch chung của một Couple cho đám cưới của họ: ngày cưới, ngân sách, checklist công việc và danh sách khách mời.

**Unset Plan** ("Kế hoạch chưa thiết lập"):
Wedding Plan được tạo sẵn khi người dùng chọn vai trò Couple, chưa có ngày cưới và ngân sách.
_Tên trong code_: `PlanStatus.DRAFT`
_Avoid_: Draft (để không lẫn với Draft Contract)

**Plan Task** ("Công việc"):
Một việc trong checklist của Wedding Plan. Tiến độ của kế hoạch là phần trăm Plan Task đã xong.

**Milestone** ("Mốc"):
Nhóm Plan Task theo khoảng thời gian tính ngược từ ngày cưới, ví dụ "Trước 6 tháng".
_Tên trong code_: `monthGroup`

**Budget Item** ("Hạng mục ngân sách"):
Một hạng mục chi tiêu của Wedding Plan, có số dự kiến và số thực chi. Trạng thái "Đã cọc" / "Đã trả đủ" do Couple tự theo dõi với bất kỳ bên cung cấp nào, không liên quan đến Deposit hay Paid in Full của Contract studio.

**Guest** ("Khách mời"):
Một người được mời dự đám cưới trong Wedding Plan, có nhóm và số bàn.

**RSVP** ("Phản hồi tham dự"):
Câu trả lời của Guest: Tham dự, Không tham dự, hoặc Chưa phản hồi.
_Tên trong code_: `GuestStatus` (`PENDING` = Chưa phản hồi)

**Partner** ("Người ghép đôi"):
Người thứ hai tham gia một Wedding Plan bằng Pairing Code. Sau khi ghép, Partner có quyền như người tạo kế hoạch.
_Avoid_: Owner (người tạo kế hoạch không phải một vai trò nghiệp vụ)

**Pairing Code** ("Mã ghép đôi"):
Mã 6 ký tự, dùng một lần trong 24 giờ, để mời Partner vào một Wedding Plan.
_Tên trong code_: `PlanSyncCode`
_Avoid_: Sync code

**Quote Request** ("Yêu cầu báo giá"):
Lời hỏi giá của một Couple cho một Service Package của vendor.

**Vendor Project** ("Dự án"):
Một Quote Request đã được chốt, tức là việc vendor sẽ thực hiện cho một Couple vào một ngày, tại một địa điểm.
_Avoid_: Booking (số lượt đặt của vendor là số Vendor Project)

## Thông báo

**Studio Feed** ("Thông báo thông minh"):
Luồng Notification chung của cả đội studio, không có người nhận riêng.

**Inbox** ("Hộp thư thông báo"):
Luồng Notification của riêng một người dùng WedPlanner, gồm thông báo AI, tin nhắn, ghép đôi và nhắc việc.
