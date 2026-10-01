import React, { useState } from 'react';
import { 
  LayoutDashboard, 
  CalendarDays, 
  Package, 
  Shirt, 
  Users, 
  HeartHandshake, 
  Bell, 
  Search, 
  Plus, 
  MoreVertical,
  TrendingUp,
  DollarSign,
  Camera,
  CheckCircle2,
  AlertCircle,
  Filter,
  MessageCircle,
  Edit,
  Phone,
  X,
  Menu,
  Trash2,
  Image as ImageIcon,
  LogOut,
  Lock,
  FileText,
  ChevronLeft,
  ChevronRight,
  Clock,
  Download,
  Share2,
  Printer,
  PieChart,
  Settings,
  Kanban,
  List,
  CreditCard,
  Building,
  Receipt
} from 'lucide-react';

// --- MOCK DATA ---
const MOCK_STATS = [
  { label: 'Doanh thu tháng', value: '245.000.000đ', trend: '+12%', icon: DollarSign, color: 'text-emerald-600', bg: 'bg-emerald-100' },
  { label: 'Hợp đồng đang chạy', value: '42', trend: '+5%', icon: CalendarDays, color: 'text-blue-600', bg: 'bg-blue-100' },
  { label: 'Khách cần tư vấn', value: '8', trend: '-2%', icon: Users, color: 'text-amber-600', bg: 'bg-amber-100' },
  { label: 'Tỉ lệ thuê trang phục', value: '78%', trend: '+15%', icon: Shirt, color: 'text-purple-600', bg: 'bg-purple-100' },
];

const MOCK_SCHEDULE = [
  { id: 1, date: '2026-06-28', time: '09:00 AM', client: 'Lan & Hoàng', type: 'Chụp Pre-wedding', status: 'In Progress', staff: 'Tuấn Photo' },
  { id: 2, date: '2026-06-28', time: '11:30 AM', client: 'Mai & Huy', type: 'Thử Váy Cưới', status: 'Upcoming', staff: 'Hương Sale' },
  { id: 3, date: '2026-06-29', time: '02:00 PM', client: 'Linh & Tuấn', type: 'Lễ Ăn Hỏi', status: 'Upcoming', staff: 'Minh Video' },
  { id: 4, date: '2026-06-30', time: '08:00 AM', client: 'Trang & Hiếu', type: 'Chụp Pre-wedding', status: 'Confirmed', staff: 'Tuấn Photo' },
];

const MOCK_PACKAGES = [
  { id: 1, name: 'Gói Kim Cương (Ngày Cưới)', price: '25.000.000đ', type: 'Trọn gói', features: ['2 Thợ Phóng sự', '1 Quay Phim', '1 Album Photobook 30x30', 'Makeup & Làm tóc trọn ngày', 'Tặng 2 Ảnh Cổng'], status: 'Active' },
  { id: 2, name: 'Gói Tiêu Chuẩn (Pre-wedding)', price: '12.500.000đ', type: 'Pre-wedding', features: ['1 Thợ Chụp', '2 Địa điểm nội thành', '2 Váy + 2 Vest', 'Makeup & Làm tóc đi kèm'], status: 'Active' },
  { id: 3, name: 'Gói Lễ Ăn Hỏi', price: '6.000.000đ', type: 'Lễ Ăn Hỏi', features: ['1 Thợ Chụp truyền thống', 'Giao toàn bộ file gốc', 'Chỉnh sửa 50 file'], status: 'Active' },
];

const MOCK_ASSETS = [
  { id: 'VAY-001', name: 'Vera Wang đuôi cá đính đá', category: 'Váy Cưới', size: 'M', status: 'Available', nextBooking: '12/10/2026', maintenanceBuffer: 3 },
  { id: 'VST-002', name: 'Vest đen Hàn Quốc', category: 'Vest', size: '42R', status: 'In Use', nextBooking: 'Đang cho thuê', maintenanceBuffer: 1 },
  { id: 'VAY-003', name: 'Soiree bồng bềnh công chúa', category: 'Váy Cưới', size: 'S', status: 'Maintenance', nextBooking: 'Đang giặt hấp', maintenanceBuffer: 3 },
  { id: 'MAY-004', name: 'Sony A7IV + Lens 35mm GM', category: 'Thiết bị', size: 'N/A', status: 'Available', nextBooking: '15/10/2026', maintenanceBuffer: 0 },
];

const MOCK_STAFF = [
  { id: 'NV-001', name: 'Tuấn Photo', role: 'Thợ Chụp Chính', phone: '0901234567', status: 'Đang làm việc', completedThisMonth: 12 },
  { id: 'NV-002', name: 'Hương Sale', role: 'Tư Vấn Viên', phone: '0987654321', status: 'Đang làm việc', completedThisMonth: 45 },
  { id: 'NV-003', name: 'Minh Video', role: 'Thợ Quay Phim', phone: '0912345678', status: 'Nghỉ phép', completedThisMonth: 8 },
  { id: 'NV-004', name: 'Trang Makeup', role: 'Chuyên Viên Trang Điểm', phone: '0977777777', status: 'Đang làm việc', completedThisMonth: 15 },
];

const MOCK_TASKS = [
  { id: 1, title: 'Chuẩn bị trang phục cho khách Lan & Hoàng', completed: false, date: '28/06/2026' },
  { id: 2, title: 'Makeup cô dâu Mai - Lễ Ăn Hỏi', completed: true, date: '27/06/2026' },
  { id: 3, title: 'Kiểm tra pin và thẻ nhớ máy Sony A7IV', completed: false, date: '28/06/2026' },
];

const MOCK_CUSTOMERS = [
  { id: 'KH-102', name: 'Nguyễn Thị Lan', phone: '0901112222', type: 'Gói Kim Cương', total: '25.000.000đ', paid: '10.000.000đ', remaining: '15.000.000đ', status: 'Đang thực hiện', hasZalo: true },
  { id: 'KH-103', name: 'Trần Văn Huy', phone: '0988889999', type: 'Gói Lễ Ăn Hỏi', total: '6.000.000đ', paid: '6.000.000đ', remaining: '0đ', status: 'Đã hoàn thành', hasZalo: true },
  { id: 'KH-104', name: 'Lê Hoàng C', phone: '0911223344', type: 'Gói Tiêu Chuẩn', total: '12.500.000đ', paid: '2.500.000đ', remaining: '10.000.000đ', status: 'Mới cọc', hasZalo: false },
];

const MOCK_LEADS = [
  { id: 'L1', name: 'Minh & Châu', phone: '0933111222', interest: 'Gói Lễ Ăn Hỏi', status: 'Mới hỏi', date: '28/06/2026' },
  { id: 'L2', name: 'Quốc & Anh', phone: '0977888999', interest: 'Gói Kim Cương', status: 'Đang tư vấn', date: '25/06/2026' },
  { id: 'L3', name: 'Thảo & Đạt', phone: '0988111333', interest: 'Gói Tiêu Chuẩn', status: 'Chờ cọc', date: '24/06/2026' },
];

const MOCK_TRANSACTIONS = [
  { id: 'TRX-001', date: '28/06/2026', desc: 'Thanh toán cọc hợp đồng Lê Hoàng C', type: 'income', amount: '2.500.000đ', category: 'Doanh thu HĐ' },
  { id: 'TRX-002', date: '27/06/2026', desc: 'In album photobook khách Mai Huy', type: 'expense', amount: '1.200.000đ', category: 'Chi phí in ấn' },
  { id: 'TRX-003', date: '26/06/2026', desc: 'Thuê xe di chuyển chụp Pre-wedding', type: 'expense', amount: '800.000đ', category: 'Chi phí vận hành' },
];

// --- REUSABLE COMPONENTS ---
const Badge = ({ children, variant = 'default' }) => {
  const variants = {
    default: 'bg-slate-100 text-slate-700',
    success: 'bg-emerald-100 text-emerald-700',
    warning: 'bg-amber-100 text-amber-700',
    danger: 'bg-red-100 text-red-700',
    brand: 'bg-rose-100 text-rose-700'
  };
  return (
    <span className={`px-2.5 py-1 rounded-full text-xs font-medium ${variants[variant]}`}>
      {children}
    </span>
  );
};

const Card = ({ children, className = '' }) => (
  <div className={`bg-white rounded-xl border border-slate-200 shadow-sm ${className}`}>
    {children}
  </div>
);

// Used for Profile details / Information panels
const SlideOver = ({ isOpen, onClose, title, children, width = "max-w-md" }) => {
  if (!isOpen) return null;
  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      <div className="absolute inset-0 bg-slate-900/50 backdrop-blur-sm transition-opacity" onClick={onClose} />
      <div className="fixed inset-y-0 right-0 w-full flex max-w-full sm:max-w-md lg:max-w-lg xl:max-w-xl">
        <div className={`w-full h-full bg-white shadow-2xl flex flex-col animate-in slide-in-from-right duration-300 ${width}`}>
          <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50">
            <h2 className="text-lg font-bold text-slate-900">{title}</h2>
            <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-200 transition-colors">
              <X size={20} />
            </button>
          </div>
          <div className="flex-1 overflow-y-auto p-6">
            {children}
          </div>
        </div>
      </div>
    </div>
  );
};

// --- NEW CENTERED MODAL FOR "CREATE" ACTIONS ---
const Modal = ({ isOpen, onClose, title, children, width = "max-w-md" }) => {
  if (!isOpen) return null;
  return (
    <div className="fixed inset-0 z-[60] overflow-hidden flex items-center justify-center p-4 sm:p-6">
      <div className="absolute inset-0 bg-slate-900/50 backdrop-blur-sm transition-opacity" onClick={onClose} />
      <div className={`relative bg-white rounded-2xl shadow-2xl w-full ${width} max-h-[90vh] flex flex-col overflow-hidden animate-in zoom-in-95 duration-200`}>
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50 rounded-t-2xl">
          <h2 className="text-lg font-bold text-slate-900">{title}</h2>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-200 transition-colors">
            <X size={20} />
          </button>
        </div>
        <div className="flex-1 overflow-y-auto p-6">
          {children}
        </div>
      </div>
    </div>
  );
};

// --- CONTRACT EXPORT MODAL ---
const ContractExportModal = ({ isOpen, onClose, customer, onExport }) => {
  if (!isOpen || !customer) return null;
  return (
    <div className="fixed inset-0 z-[60] overflow-hidden flex items-center justify-center p-4 sm:p-6">
      <div className="absolute inset-0 bg-slate-900/60 backdrop-blur-sm transition-opacity" onClick={onClose} />
      <div className="relative bg-slate-100 rounded-2xl shadow-2xl w-full max-w-3xl max-h-[90vh] flex flex-col overflow-hidden animate-in zoom-in-95 duration-300">
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-white z-10">
          <h2 className="text-lg font-bold text-slate-900 flex items-center"><FileText className="mr-2" size={20}/> Xuất Hợp Đồng</h2>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100 transition-colors">
            <X size={20} />
          </button>
        </div>
        
        <div className="flex-1 overflow-y-auto p-4 sm:p-8 flex justify-center">
          {/* A4 Paper Mockup */}
          <div className="bg-white p-8 sm:p-12 shadow-md w-full max-w-[210mm] min-h-[297mm] text-slate-800 font-serif relative">
            <div className="absolute top-0 left-0 w-full h-2 bg-rose-600"></div>
            <div className="flex justify-between items-start border-b-2 border-slate-100 pb-6 mb-6">
              <div className="flex items-center gap-3">
                <div className="bg-rose-600 text-white p-2 rounded-lg"><Camera size={24} /></div>
                <div>
                  <h1 className="font-bold text-2xl tracking-tight text-slate-900">LUMIÈRE STUDIOS</h1>
                  <p className="text-xs text-slate-500 uppercase tracking-widest">Lưu Giữ Khoảnh Khắc</p>
                </div>
              </div>
              <div className="text-right text-sm">
                <p>Số HĐ: <strong>HD-{customer.id}</strong></p>
                <p>Ngày lập: 28/06/2026</p>
              </div>
            </div>
            
            <h2 className="text-xl font-bold text-center mb-8 uppercase tracking-wider text-slate-900">HỢP ĐỒNG DỊCH VỤ CƯỚI</h2>
            
            <div className="space-y-6 text-sm">
              <div>
                <h3 className="font-bold text-slate-900 border-b border-slate-200 pb-1 mb-2">ĐẠI DIỆN KHÁCH HÀNG (BÊN A)</h3>
                <div className="grid grid-cols-2 gap-2">
                  <p><strong>Họ và tên:</strong> {customer.name}</p>
                  <p><strong>Số điện thoại:</strong> {customer.phone}</p>
                </div>
              </div>

              <div>
                <h3 className="font-bold text-slate-900 border-b border-slate-200 pb-1 mb-2">CHI TIẾT DỊCH VỤ (BÊN B)</h3>
                <p className="mb-2"><strong>Gói dịch vụ đăng ký:</strong> {customer.type}</p>
                <table className="w-full border-collapse border border-slate-300 mt-2">
                  <thead>
                    <tr className="bg-slate-50">
                      <th className="border border-slate-300 p-2 text-left">Nội dung</th>
                      <th className="border border-slate-300 p-2 text-right">Thành tiền</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td className="border border-slate-300 p-2">{customer.type}</td>
                      <td className="border border-slate-300 p-2 text-right font-medium">{customer.total}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div>
                <h3 className="font-bold text-slate-900 border-b border-slate-200 pb-1 mb-2">ĐIỀU KHOẢN THANH TOÁN</h3>
                <div className="space-y-1">
                  <p className="flex justify-between"><span>Tổng giá trị hợp đồng:</span> <strong className="text-lg">{customer.total}</strong></p>
                  <p className="flex justify-between text-emerald-700"><span>Đã thanh toán (Cọc):</span> <strong>{customer.paid}</strong></p>
                  <p className="flex justify-between text-rose-600 border-t border-slate-200 pt-1 mt-1"><span>Số tiền còn lại (Thanh toán trước ngày nhận ảnh):</span> <strong>{customer.remaining}</strong></p>
                </div>
              </div>
            </div>

            <div className="mt-16 flex justify-between px-8 text-center">
              <div>
                <p className="font-bold text-slate-900 mb-16">Đại diện Khách hàng</p>
                <p className="text-sm text-slate-500">(Ký và ghi rõ họ tên)</p>
              </div>
              <div>
                <p className="font-bold text-slate-900 mb-16">Đại diện Studio</p>
                <p className="text-sm text-slate-500">(Ký và ghi rõ họ tên)</p>
              </div>
            </div>
          </div>
        </div>

        <div className="p-4 bg-white border-t border-slate-200 flex justify-end gap-3 z-10">
          <button 
            onClick={() => onExport('zalo', customer)}
            className="px-4 py-2 bg-blue-50 text-blue-600 hover:bg-blue-100 font-medium rounded-lg flex items-center transition-colors"
          >
            <Share2 size={18} className="mr-2" /> Chia sẻ Zalo
          </button>
          <button 
            onClick={() => onExport('pdf', customer)}
            className="px-4 py-2 bg-rose-600 text-white hover:bg-rose-700 font-medium rounded-lg flex items-center transition-colors shadow-sm"
          >
            <Download size={18} className="mr-2" /> Tải xuống PDF
          </button>
        </div>
      </div>
    </div>
  );
};

// --- MAIN VIEWS ---
const DashboardView = ({ user, setGlobalOpenTab }) => (
  <div className="space-y-6">
    <div className="bg-white p-6 rounded-xl border border-slate-200 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h2 className="text-2xl font-bold text-slate-900">Xin chào, {user?.name || 'Admin'}! 👋</h2>
        <p className="text-slate-500 mt-1">Chúc bạn một ngày làm việc hiệu quả. Dưới đây là tổng quan tình hình studio.</p>
      </div>
      
      {/* Quick Actions UX Enhancement */}
      <div className="flex gap-2 overflow-x-auto pb-2 md:pb-0">
        <button onClick={() => setGlobalOpenTab('contracts')} className="shrink-0 flex items-center px-4 py-2 bg-rose-50 hover:bg-rose-100 text-rose-700 rounded-lg text-sm font-bold transition-colors">
          <Plus size={16} className="mr-2" /> Tạo Hợp Đồng
        </button>
        <button onClick={() => setGlobalOpenTab('bookings')} className="shrink-0 flex items-center px-4 py-2 bg-blue-50 hover:bg-blue-100 text-blue-700 rounded-lg text-sm font-bold transition-colors">
          <CalendarDays size={16} className="mr-2" /> Thêm Lịch
        </button>
        <button onClick={() => setGlobalOpenTab('financials')} className="shrink-0 flex items-center px-4 py-2 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 rounded-lg text-sm font-bold transition-colors">
          <CreditCard size={16} className="mr-2" /> Thu Tiền
        </button>
      </div>
    </div>

    {user?.role === 'admin' && (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {MOCK_STATS.map((stat, idx) => (
          <Card key={idx} className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-slate-500">{stat.label}</p>
                <h3 className="text-2xl font-bold text-slate-900 mt-1">{stat.value}</h3>
              </div>
              <div className={`w-12 h-12 rounded-full flex items-center justify-center ${stat.bg} ${stat.color}`}>
                <stat.icon size={24} />
              </div>
            </div>
            <div className="mt-4 flex items-center text-sm">
              <TrendingUp size={16} className={`mr-1 ${stat.trend.startsWith('+') ? 'text-emerald-500' : 'text-red-500'}`} />
              <span className={stat.trend.startsWith('+') ? 'text-emerald-600 font-medium' : 'text-red-600 font-medium'}>
                {stat.trend}
              </span>
              <span className="text-slate-500 ml-2">so với tháng trước</span>
            </div>
          </Card>
        ))}
      </div>
    )}

    <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
      {user?.role === 'admin' && (
        <Card className="p-6 lg:col-span-2">
          <div className="flex justify-between items-center mb-6">
            <h3 className="text-lg font-bold text-slate-900">Tổng quan doanh thu</h3>
            <select className="bg-slate-50 border border-slate-200 text-sm rounded-lg px-3 py-1.5 outline-none focus:ring-2 focus:ring-rose-500">
              <option>Năm nay</option>
              <option>Năm ngoái</option>
            </select>
          </div>
          <div className="h-64 flex items-end justify-between gap-2 pt-4">
            {[40, 70, 45, 90, 65, 85, 100, 50, 75, 60, 80, 95].map((height, i) => (
              <div key={i} className="w-full bg-slate-100 rounded-t-sm relative group h-full flex items-end">
                <div 
                  className="w-full bg-rose-200 hover:bg-rose-400 transition-colors rounded-t-sm" 
                  style={{ height: `${height}%` }}
                ></div>
                <div className="opacity-0 group-hover:opacity-100 absolute -top-8 left-1/2 -translate-x-1/2 bg-slate-800 text-white text-xs py-1 px-2 rounded pointer-events-none transition-opacity whitespace-nowrap z-10">
                  {height} Triệu
                </div>
              </div>
            ))}
          </div>
          <div className="flex justify-between mt-2 text-xs text-slate-400 font-medium px-1">
            <span>T1</span><span>T2</span><span>T3</span><span>T4</span><span>T5</span><span>T6</span>
            <span>T7</span><span>T8</span><span>T9</span><span>T10</span><span>T11</span><span>T12</span>
          </div>
        </Card>
      )}

      <Card className={`p-6 ${user?.role === 'staff' ? 'lg:col-span-3' : ''}`}>
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-lg font-bold text-slate-900">Lịch trình hôm nay</h3>
          <button onClick={() => setGlobalOpenTab('bookings')} className="text-rose-600 text-sm font-medium hover:text-rose-700">Xem tất cả</button>
        </div>
        <div className={`space-y-4 ${user?.role === 'staff' ? 'grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 space-y-0' : ''}`}>
          {MOCK_SCHEDULE.slice(0, user?.role === 'staff' ? 6 : 3).map((item) => (
            <div key={item.id} className="flex gap-4 p-4 rounded-lg border border-slate-100 hover:border-rose-100 hover:bg-rose-50/30 transition-colors">
              <div className="flex flex-col items-center justify-center min-w-[60px] border-r border-slate-100 pr-4">
                <span className="text-sm font-bold text-slate-900">{item.time.split(' ')[0]}</span>
                <span className="text-xs text-slate-500">{item.time.split(' ')[1]}</span>
              </div>
              <div>
                <p className="text-sm font-bold text-slate-900">{item.client}</p>
                <p className="text-xs text-slate-500 mb-2">{item.type}</p>
                <div className="flex items-center gap-2">
                  <Badge variant={item.status === 'In Progress' ? 'brand' : 'default'}>{item.status}</Badge>
                  {user?.role === 'admin' && (
                    <span className="text-xs text-slate-400 flex items-center"><Users size={12} className="mr-1"/> {item.staff}</span>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  </div>
);

const BookingsView = ({ showToast }) => {
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedDate, setSelectedDate] = useState('2026-06-28');
  const [calendarView, setCalendarView] = useState('day'); // 'day', 'week', 'month'

  // Calendar logic
  const daysInMonth = Array.from({ length: 30 }, (_, i) => {
    const day = i + 1;
    const dateStr = `2026-06-${day.toString().padStart(2, '0')}`;
    const hasBooking = MOCK_SCHEDULE.some(s => s.date === dateStr);
    return { day, dateStr, hasBooking };
  });

  const selectedDaySchedules = MOCK_SCHEDULE.filter(s => s.date === selectedDate);

  return (
    <div className="space-y-6 h-full flex flex-col">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Quản lý Lịch trình</h2>
          <p className="text-slate-500 text-sm">Theo dõi lịch chụp, thử váy, và các cuộc hẹn khách hàng.</p>
        </div>
        <div className="flex gap-2">
          {/* Advanced Calendar Views Toggle */}
          <div className="bg-slate-100 p-1 rounded-lg flex text-sm font-medium mr-2">
            <button onClick={() => setCalendarView('day')} className={`px-3 py-1.5 rounded-md transition-colors ${calendarView === 'day' ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500'}`}>Ngày</button>
            <button onClick={() => setCalendarView('week')} className={`px-3 py-1.5 rounded-md transition-colors ${calendarView === 'week' ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500'}`}>Tuần</button>
            <button onClick={() => setCalendarView('month')} className={`px-3 py-1.5 rounded-md transition-colors ${calendarView === 'month' ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500'}`}>Tháng</button>
          </div>
          <button onClick={() => setIsCreateOpen(true)} className="bg-rose-600 hover:bg-rose-700 text-white px-4 py-2 rounded-lg flex items-center font-medium transition-colors">
            <Plus size={18} className="mr-2" /> Thêm Lịch Mới
          </button>
        </div>
      </div>

      <div className="flex flex-col lg:flex-row gap-6 flex-1">
        {/* Calendar Sidebar */}
        <Card className="p-6 lg:w-96 shrink-0 flex flex-col">
          <div className="flex justify-between items-center mb-6">
            <button className="p-2 hover:bg-slate-100 rounded-lg text-slate-600"><ChevronLeft size={20} /></button>
            <h3 className="text-lg font-bold text-slate-900">Tháng 6, 2026</h3>
            <button className="p-2 hover:bg-slate-100 rounded-lg text-slate-600"><ChevronRight size={20} /></button>
          </div>
          
          <div className="grid grid-cols-7 gap-1 text-center mb-2">
            {['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'].map(day => (
              <div key={day} className="text-xs font-bold text-slate-400 py-2">{day}</div>
            ))}
          </div>

          <div className="grid grid-cols-7 gap-1">
            <div className="p-2"></div>
            {daysInMonth.map(({ day, dateStr, hasBooking }) => (
              <button
                key={day}
                onClick={() => setSelectedDate(dateStr)}
                className={`relative p-2 h-10 w-full rounded-lg text-sm font-medium flex items-center justify-center transition-all
                  ${selectedDate === dateStr ? 'bg-rose-600 text-white shadow-md border-rose-600' : 'hover:bg-slate-100 text-slate-700 border-transparent'}
                  ${hasBooking && selectedDate !== dateStr ? 'bg-rose-50 text-rose-700 font-bold border border-rose-100' : ''}
                `}
              >
                {day}
                {hasBooking && selectedDate !== dateStr && (
                  <span className="absolute bottom-1 w-1 h-1 bg-rose-500 rounded-full"></span>
                )}
              </button>
            ))}
          </div>

          <div className="mt-auto pt-6 border-t border-slate-100 flex items-center gap-4 text-xs text-slate-500">
            <div className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-slate-100 border border-slate-300"></span> Trống lịch</div>
            <div className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-rose-50 border border-rose-200 flex items-center justify-center"><span className="w-1 h-1 bg-rose-500 rounded-full"></span></span> Có lịch hẹn</div>
          </div>
        </Card>

        {/* Schedule List for Selected Date */}
        <Card className="flex-1 p-0 overflow-hidden flex flex-col">
          <div className="border-b border-slate-200 p-6 bg-slate-50 flex items-center gap-3">
            <CalendarDays className="text-rose-600" size={24} />
            <div>
              <h3 className="text-lg font-bold text-slate-900">
                {calendarView === 'day' ? `Lịch trình ngày ${selectedDate.split('-').reverse().join('/')}` : `Hiển thị lịch dạng ${calendarView === 'week' ? 'Tuần' : 'Tháng'}`}
              </h3>
              <p className="text-sm text-slate-500">{selectedDaySchedules.length} công việc được xếp lịch</p>
            </div>
          </div>

          <div className="p-6 overflow-y-auto flex-1 bg-slate-50/50">
            {selectedDaySchedules.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-full text-slate-400 space-y-3 py-12">
                <Clock size={48} className="text-slate-200" />
                <p>Không có lịch trình nào cho ngày này.</p>
                <button onClick={() => setIsCreateOpen(true)} className="text-rose-600 font-medium hover:underline text-sm">
                  + Thêm lịch mới ngay
                </button>
              </div>
            ) : (
              <div className="space-y-4">
                {selectedDaySchedules.map((booking, idx) => (
                  <div key={idx} className="flex flex-col md:flex-row gap-4 md:gap-6 p-4 rounded-xl border border-slate-200 hover:shadow-md transition-shadow bg-white relative overflow-hidden">
                    <div className="absolute left-0 top-0 bottom-0 w-1 bg-rose-500"></div>
                    <div className="md:w-32 shrink-0 flex flex-col justify-center items-center p-3 bg-slate-50 rounded-lg border border-slate-100">
                      <span className="text-lg text-slate-900 font-bold">{booking.time.split(' ')[0]}</span>
                      <span className="text-xs text-slate-500 mt-1 font-medium">{booking.time.split(' ')[1]}</span>
                    </div>
                    <div className="flex-1 flex flex-col justify-center">
                      <div className="flex items-start justify-between">
                        <div>
                          <h4 className="text-base font-bold text-slate-900">Khách: {booking.client}</h4>
                          <p className="text-sm text-rose-600 font-medium flex items-center mt-1">
                            <Camera size={14} className="mr-1" /> {booking.type}
                          </p>
                        </div>
                        <Badge variant={booking.status === 'Confirmed' ? 'success' : 'warning'}>{booking.status}</Badge>
                      </div>
                      <div className="mt-4 pt-3 border-t border-slate-100 flex flex-wrap gap-4 text-sm text-slate-500">
                        <span className="flex items-center"><Users size={14} className="mr-1.5" /> Phụ trách: <strong>{booking.staff}</strong></span>
                        <span className="flex items-center"><Package size={14} className="mr-1.5" /> Gói DV: Kim Cương</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </Card>
      </div>

      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Thêm Lịch Trình Mới">
        <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); setIsCreateOpen(false); showToast('Đã thêm lịch trình thành công!'); }}>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Tên khách hàng <span className="text-red-500">*</span></label>
            <input type="text" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none" placeholder="VD: Lan & Hoàng" required />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Ngày hẹn <span className="text-red-500">*</span></label>
              <input type="date" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none" required />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Giờ hẹn <span className="text-red-500">*</span></label>
              <input type="time" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none" required />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Loại lịch hẹn</label>
            <select className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none">
              <option>Chụp Pre-wedding</option>
              <option>Chụp Phóng sự cưới</option>
              <option>Thử Váy / Vest</option>
              <option>Tư vấn hợp đồng</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Nhân viên phụ trách</label>
            <select className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none">
              <option>Tuấn Photo</option>
              <option>Hương Sale</option>
              <option>Trang Makeup</option>
            </select>
          </div>
          <button type="submit" className="w-full bg-rose-600 hover:bg-rose-700 text-white py-2 rounded-lg font-medium transition-colors mt-4">
            Lưu Lịch Trình
          </button>
        </form>
      </Modal>
    </div>
  );
};

const PackagesView = ({ showToast }) => {
  const [isCreateOpen, setIsCreateOpen] = useState(false);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Quản lý Gói Dịch Vụ</h2>
          <p className="text-slate-500 text-sm">Thiết lập giá và chi tiết các gói chụp, makeup, thuê đồ.</p>
        </div>
        <button onClick={() => setIsCreateOpen(true)} className="bg-rose-600 hover:bg-rose-700 text-white px-4 py-2 rounded-lg flex items-center font-medium transition-colors">
          <Plus size={18} className="mr-2" /> Tạo Gói Mới
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {MOCK_PACKAGES.map((pkg) => (
          <Card key={pkg.id} className="flex flex-col overflow-hidden hover:border-rose-300 transition-colors">
            <div className="h-32 bg-slate-100 relative p-6 flex flex-col justify-end overflow-hidden">
              <div className="absolute inset-0 bg-gradient-to-br from-rose-100 to-slate-100 opacity-50"></div>
              <div className="relative z-10 flex justify-between items-end">
                <Badge variant="brand">{pkg.type}</Badge>
                <span className="text-2xl font-bold text-slate-900">{pkg.price}</span>
              </div>
            </div>
            <div className="p-6 flex-1 flex flex-col">
              <div className="flex justify-between items-start mb-4">
                <h3 className="text-lg font-bold text-slate-900 leading-tight">{pkg.name}</h3>
              </div>
              <ul className="space-y-2 mb-6 flex-1">
                {pkg.features.map((feature, idx) => (
                  <li key={idx} className="flex items-start text-sm text-slate-600">
                    <CheckCircle2 size={16} className="textemerald-500 mr-2 mt-0.5 shrink-0" />
                    {feature}
                  </li>
                ))}
              </ul>
              <button className="w-full py-2 border border-slate-200 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors flex items-center justify-center">
                <Edit size={16} className="mr-2"/> Chỉnh sửa gói
              </button>
            </div>
          </Card>
        ))}
      </div>

      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Tạo Gói Dịch Vụ Mới">
        <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); setIsCreateOpen(false); showToast('Đã tạo gói dịch vụ mới!'); }}>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Tên gói dịch vụ <span className="text-red-500">*</span></label>
            <input type="text" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none" placeholder="VD: Gói Chụp Phóng Sự" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Giá trọn gói <span className="text-red-500">*</span></label>
            <input type="text" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none" placeholder="VD: 10.000.000đ" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Loại dịch vụ</label>
            <select className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none">
              <option>Trọn gói</option>
              <option>Pre-wedding</option>
              <option>Ngày Cưới / Lễ</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Mô tả chi tiết (Mỗi dòng 1 mục)</label>
            <textarea rows={4} className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none" placeholder="1 Thợ Chụp..."></textarea>
          </div>
          <button type="submit" className="w-full bg-rose-600 hover:bg-rose-700 text-white py-2 rounded-lg font-medium transition-colors mt-4">
            Lưu Gói Dịch Vụ
          </button>
        </form>
      </Modal>
    </div>
  );
};

const AssetsView = ({ showToast }) => {
  const [isCreateOpen, setIsCreateOpen] = useState(false);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Kho Đồ & Thiết Bị</h2>
          <p className="text-slate-500 text-sm">Quản lý Váy Cưới, Vest, và thiết bị chụp ảnh để tránh trùng lịch.</p>
        </div>
        <div className="flex gap-2">
          <button className="border border-slate-200 hover:bg-slate-50 text-slate-700 px-4 py-2 rounded-lg flex items-center font-medium transition-colors">
            <Filter size={18} className="mr-2" /> Lọc
          </button>
          <button onClick={() => setIsCreateOpen(true)} className="bg-rose-600 hover:bg-rose-700 text-white px-4 py-2 rounded-lg flex items-center font-medium transition-colors">
            <Plus size={18} className="mr-2" /> Thêm Đồ
          </button>
        </div>
      </div>

      <div className="bg-amber-50 border border-amber-200 rounded-lg p-4 flex items-start gap-3">
        <AlertCircle className="text-amber-600 shrink-0 mt-0.5" size={20} />
        <div>
          <h4 className="text-sm font-bold text-amber-800">Cảnh báo: Trùng lịch trang phục</h4>
          <p className="text-sm text-amber-700 mt-1">Sản phẩm "VAY-001 (Vera Wang đuôi cá)" đang được xếp cho 2 cô dâu vào ngày 12/10. Vui lòng kiểm tra lại lịch.</p>
        </div>
      </div>

      <Card className="overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200">
                <th className="py-3 px-6 text-xs font-semibold text-slate-500 uppercase tracking-wider">Mã / Tên SP</th>
                <th className="py-3 px-6 text-xs font-semibold text-slate-500 uppercase tracking-wider">Danh mục & Size</th>
                <th className="py-3 px-6 text-xs font-semibold text-slate-500 uppercase tracking-wider">Trạng thái</th>
                <th className="py-3 px-6 text-xs font-semibold text-slate-500 uppercase tracking-wider">Lịch tiếp theo & Buffer</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {MOCK_ASSETS.map((asset) => (
                <tr key={asset.id} className="hover:bg-slate-50/50 transition-colors">
                  <td className="py-4 px-6">
                    <div className="text-sm font-bold text-slate-900">{asset.name}</div>
                    <div className="text-xs text-slate-500 font-mono mt-0.5">{asset.id}</div>
                  </td>
                  <td className="py-4 px-6">
                    <div className="text-sm text-slate-700">{asset.category}</div>
                    <div className="text-xs text-slate-500 mt-0.5">Size: {asset.size}</div>
                  </td>
                  <td className="py-4 px-6">
                    <Badge 
                      variant={
                        asset.status === 'Available' ? 'success' : 
                        asset.status === 'In Use' ? 'brand' : 'warning'
                      }
                    >
                      {asset.status === 'Available' ? 'Sẵn sàng' : asset.status === 'In Use' ? 'Đang thuê' : 'Bảo trì'}
                    </Badge>
                  </td>
                  <td className="py-4 px-6">
                    <div className="text-sm text-slate-700 flex items-center">
                      <CalendarDays size={14} className="mr-2 text-slate-400" />
                      {asset.nextBooking}
                    </div>
                    {asset.maintenanceBuffer > 0 && (
                      <div className="text-[10px] text-amber-600 font-medium mt-1 flex items-center">
                        <Clock size={10} className="mr-1" /> Block thêm {asset.maintenanceBuffer} ngày giặt hấp
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Thêm Sản Phẩm Mới">
        <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); setIsCreateOpen(false); showToast('Đã thêm sản phẩm mới!'); }}>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Mã sản phẩm <span className="text-red-500">*</span></label>
            <input type="text" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none" placeholder="VD: VAY-005" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Tên sản phẩm <span className="text-red-500">*</span></label>
            <input type="text" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none" required />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Danh mục</label>
              <select className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none">
                <option>Váy Cưới</option>
                <option>Vest</option>
                <option>Áo Dài</option>
                <option>Thiết Bị</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Kích cỡ</label>
              <input type="text" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none" placeholder="S, M, L..." />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Thời gian bảo dưỡng / giặt hấp (Ngày)</label>
            <input type="number" min="0" defaultValue="1" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none" placeholder="Hệ thống sẽ tự động block những ngày này sau khi thuê" />
          </div>
          <button type="submit" className="w-full bg-rose-600 hover:bg-rose-700 text-white py-2 rounded-lg font-medium transition-colors mt-4">
            Lưu Sản Phẩm
          </button>
        </form>
      </Modal>
    </div>
  );
};

const StaffView = ({ showToast }) => {
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isCreateTaskOpen, setIsCreateTaskOpen] = useState(false);
  const [selectedStaff, setSelectedStaff] = useState(null);

  return (
    <div className="space-y-6 relative">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Quản Lý Nhân Sự</h2>
          <p className="text-slate-500 text-sm">Quản lý hồ sơ nhân viên, thợ chụp, chuyên viên makeup.</p>
        </div>
        <button onClick={() => setIsCreateOpen(true)} className="bg-rose-600 hover:bg-rose-700 text-white px-4 py-2 rounded-lg flex items-center font-medium transition-colors">
          <Plus size={18} className="mr-2" /> Thêm Nhân Sự
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {MOCK_STAFF.map(staff => (
          <Card key={staff.id} className="p-6 text-center hover:shadow-md transition-shadow">
            <div className="w-20 h-20 mx-auto bg-slate-100 rounded-full flex items-center justify-center mb-4">
              <Users size={32} className="text-slate-400" />
            </div>
            <h3 className="text-lg font-bold text-slate-900">{staff.name}</h3>
            <p className="text-sm text-rose-600 font-medium mb-3">{staff.role}</p>
            <Badge variant={staff.status === 'Đang làm việc' ? 'success' : 'warning'}>{staff.status}</Badge>
            
            <div className="mt-4 pt-4 border-t border-slate-100 grid grid-cols-1 gap-2">
              <button 
                onClick={() => setSelectedStaff(staff)}
                className="w-full py-2 bg-slate-50 hover:bg-rose-50 text-slate-700 hover:text-rose-700 text-sm font-medium rounded-lg transition-colors"
              >
                Chi tiết & Giao Việc
              </button>
            </div>
          </Card>
        ))}
      </div>

      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Thêm Nhân Sự Mới">
        <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); setIsCreateOpen(false); showToast('Đã thêm nhân sự thành công!'); }}>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Họ và tên <span className="text-red-500">*</span></label>
            <input type="text" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Số điện thoại <span className="text-red-500">*</span></label>
            <input type="tel" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Vị trí công việc</label>
            <select className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none">
              <option>Thợ Chụp Chính</option>
              <option>Thợ Phụ / Ánh Sáng</option>
              <option>Chuyên Viên Makeup</option>
              <option>Tư Vấn Viên</option>
            </select>
          </div>
          <button type="submit" className="w-full bg-rose-600 hover:bg-rose-700 text-white py-2 rounded-lg font-medium transition-colors mt-4">
            Lưu Hồ Sơ
          </button>
        </form>
      </Modal>

      <SlideOver isOpen={!!selectedStaff} onClose={() => setSelectedStaff(null)} title="Hồ Sơ Nhân Sự" width="max-w-xl">
        {selectedStaff && (
          <div className="space-y-6">
            <div className="flex items-center gap-4 pb-6 border-b border-slate-200">
              <div className="w-16 h-16 bg-rose-100 rounded-full flex items-center justify-center">
                <Users size={32} className="text-rose-600" />
              </div>
              <div>
                <h2 className="text-xl font-bold text-slate-900">{selectedStaff.name}</h2>
                <p className="text-sm text-slate-500">{selectedStaff.role} • {selectedStaff.id}</p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="bg-slate-50 p-4 rounded-xl border border-slate-200">
                <p className="text-xs font-medium text-slate-500 uppercase">Liên hệ</p>
                <p className="text-sm font-bold text-slate-900 mt-1 flex items-center"><Phone size={14} className="mr-2 text-slate-400"/> {selectedStaff.phone}</p>
              </div>
              <div className="bg-slate-50 p-4 rounded-xl border border-slate-200">
                <p className="text-xs font-medium text-slate-500 uppercase">KPI Tháng này</p>
                <p className="text-sm font-bold text-slate-900 mt-1 flex items-center"><CheckCircle2 size={14} className="mr-2 text-emerald-500"/> Hoàn thành {selectedStaff.completedThisMonth} show</p>
              </div>
            </div>

            <div className="pt-4 border-t border-slate-200">
              <div className="flex justify-between items-center mb-4">
                <h3 className="font-bold text-slate-900 flex items-center">
                  <Clock className="mr-2 text-rose-600" size={18} /> Giao Việc & Theo Dõi
                </h3>
                <button 
                  onClick={() => setIsCreateTaskOpen(true)}
                  className="text-xs font-medium text-rose-600 bg-rose-50 px-3 py-1.5 rounded-lg hover:bg-rose-100"
                >
                  + Giao việc mới
                </button>
              </div>

              <div className="space-y-3">
                {MOCK_TASKS.map(task => (
                  <div key={task.id} className="flex items-start gap-3 p-3 rounded-lg border border-slate-200 bg-white hover:border-rose-300 transition-colors">
                    <input 
                      type="checkbox" 
                      defaultChecked={task.completed} 
                      className="mt-1 w-4 h-4 text-rose-600 rounded border-slate-300 focus:ring-rose-600"
                    />
                    <div className="flex-1">
                      <p className={`text-sm font-medium ${task.completed ? 'text-slate-400 line-through' : 'text-slate-800'}`}>
                        {task.title}
                      </p>
                      <p className="text-xs text-slate-400 mt-0.5">Hạn chót: {task.date}</p>
                    </div>
                    <button className="text-slate-400 hover:text-red-500"><Trash2 size={16} /></button>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </SlideOver>

      <Modal isOpen={isCreateTaskOpen} onClose={() => setIsCreateTaskOpen(false)} title="Giao Việc Mới">
        <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); setIsCreateTaskOpen(false); showToast('Đã giao việc thành công!'); }}>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Tên công việc <span className="text-red-500">*</span></label>
            <input type="text" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none" placeholder="VD: Chuẩn bị váy Vera Wang, makeup cô dâu..." required/>
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Hạn chót hoàn thành <span className="text-red-500">*</span></label>
            <input type="date" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none" required/>
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Ghi chú thêm</label>
            <textarea rows={3} className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none"></textarea>
          </div>
          <button type="submit" className="w-full bg-rose-600 hover:bg-rose-700 text-white py-2 rounded-lg font-medium transition-colors mt-4">
            Lưu Công Việc
          </button>
        </form>
      </Modal>
    </div>
  );
};

const CustomersView = ({ showToast }) => {
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [exportingCustomer, setExportingCustomer] = useState(null);
  const [viewType, setViewType] = useState('table'); // CRM toggle 'table' | 'kanban'

  const handleExport = (type, customer) => {
    setExportingCustomer(null);
    if (type === 'pdf') {
      showToast(`Đã lưu PDF hợp đồng của ${customer.name} vào máy.`);
    } else {
      showToast(`Đã mở Zalo để gửi bản mềm cho ${customer.name}.`, 'info');
    }
  };

  return (
    <div className="space-y-6 h-full flex flex-col">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Khách Hàng (CRM)</h2>
          <p className="text-slate-500 text-sm">Quản lý Lead (Khách tiềm năng) & Hợp đồng đang chạy.</p>
        </div>
        <div className="flex gap-2">
          {/* View Toggle */}
          <div className="bg-slate-100 p-1 rounded-lg flex text-sm font-medium mr-2">
            <button onClick={() => setViewType('kanban')} className={`px-3 py-1.5 rounded-md transition-colors flex items-center ${viewType === 'kanban' ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500'}`}>
              <Kanban size={16} className="mr-1.5" /> Pipeline Lead
            </button>
            <button onClick={() => setViewType('table')} className={`px-3 py-1.5 rounded-md transition-colors flex items-center ${viewType === 'table' ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500'}`}>
              <List size={16} className="mr-1.5" /> Hợp Đồng
            </button>
          </div>
          <button onClick={() => setIsCreateOpen(true)} className="bg-rose-600 hover:bg-rose-700 text-white px-4 py-2 rounded-lg flex items-center font-medium transition-colors shadow-sm shadow-rose-200">
            <Plus size={18} className="mr-2" /> Thêm Mới
          </button>
        </div>
      </div>

      {viewType === 'kanban' ? (
        /* KANBAN CRM PIPELINE */
        <div className="flex-1 overflow-x-auto pb-4">
          <div className="flex gap-6 h-full min-w-max">
            {['Mới hỏi', 'Đang tư vấn', 'Chờ cọc', 'Đã chốt (Win)', 'Hủy (Loss)'].map(status => (
              <div key={status} className="w-72 bg-slate-100/80 rounded-xl flex flex-col p-4">
                <h4 className="font-bold text-slate-700 mb-4 flex items-center justify-between">
                  {status} 
                  <span className="bg-white text-slate-500 text-xs px-2 py-0.5 rounded-full shadow-sm">
                    {MOCK_LEADS.filter(l => l.status === status).length}
                  </span>
                </h4>
                <div className="flex-1 overflow-y-auto space-y-3">
                  {MOCK_LEADS.filter(l => l.status === status).length === 0 ? (
                    <div className="text-center py-6 border-2 border-dashed border-slate-200 rounded-lg text-slate-400 text-sm">
                      Kéo thả lead vào đây
                    </div>
                  ) : (
                    MOCK_LEADS.filter(l => l.status === status).map(lead => (
                      <Card key={lead.id} className="p-4 cursor-pointer hover:border-rose-300 hover:shadow-md transition-all group">
                        <div className="flex justify-between items-start mb-2">
                          <p className="font-bold text-sm text-slate-900 group-hover:text-rose-600 transition-colors">{lead.name}</p>
                          <span className="text-[10px] text-slate-400">{lead.date}</span>
                        </div>
                        <p className="text-xs text-slate-500 flex items-center"><Phone size={12} className="mr-1" /> {lead.phone}</p>
                        <div className="mt-3 pt-3 border-t border-slate-100">
                          <Badge variant="default">{lead.interest}</Badge>
                        </div>
                      </Card>
                    ))
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : (
        /* ACTIVE CONTRACTS TABLE */
        <Card className="overflow-hidden shadow-sm border-slate-200">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200">
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Thông tin Khách</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Gói Dịch Vụ</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Thanh Toán</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider">Trạng Thái</th>
                  <th className="py-4 px-6 text-xs font-bold text-slate-500 uppercase tracking-wider text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {MOCK_CUSTOMERS.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="py-12 text-center text-slate-400">
                      <HeartHandshake size={48} className="mx-auto mb-3 text-slate-200" />
                      Chưa có hợp đồng nào.
                    </td>
                  </tr>
                ) : (
                  MOCK_CUSTOMERS.map((customer) => (
                    <tr key={customer.id} className="hover:bg-slate-50/50 transition-colors group">
                      <td className="py-4 px-6">
                        <div className="text-sm font-bold text-slate-900 flex items-center">
                          {customer.name}
                          {customer.hasZalo && (
                            <span className="ml-2 bg-blue-100 text-blue-700 text-[10px] px-1.5 py-0.5 rounded font-bold uppercase tracking-wide">Zalo</span>
                          )}
                        </div>
                        <div className="text-xs text-slate-500 mt-1 flex items-center">
                          <Phone size={12} className="mr-1"/> {customer.phone}
                        </div>
                      </td>
                      <td className="py-4 px-6">
                        <div className="text-sm font-medium text-slate-700">{customer.type}</div>
                        <div className="text-xs text-slate-400 mt-1 font-mono">{customer.id}</div>
                      </td>
                      <td className="py-4 px-6">
                        <div className="text-sm font-bold text-slate-900">{customer.total}</div>
                        <div className="text-xs text-rose-600 mt-1 font-medium">Còn nợ: {customer.remaining}</div>
                      </td>
                      <td className="py-4 px-6">
                        <Badge variant={customer.status === 'Đã hoàn thành' ? 'success' : customer.status === 'Đang thực hiện' ? 'brand' : 'warning'}>
                          {customer.status}
                        </Badge>
                      </td>
                      <td className="py-4 px-6 text-right space-x-2 flex justify-end">
                        <button 
                          onClick={() => setExportingCustomer(customer)}
                          className="p-2 bg-rose-50 text-rose-600 rounded-lg hover:bg-rose-100 transition-colors" 
                          title="Xuất hợp đồng"
                        >
                          <Printer size={16} />
                        </button>
                        {customer.hasZalo && (
                          <button className="p-2 bg-blue-50 text-blue-600 rounded-lg hover:bg-blue-100 transition-colors" title="Nhắn tin Zalo">
                            <MessageCircle size={16} />
                          </button>
                        )}
                        <button className="p-2 bg-slate-50 text-slate-600 rounded-lg hover:bg-slate-100 transition-colors" title="Chỉnh sửa">
                          <Edit size={16} />
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Thêm Mới Khách / Lead">
        <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); setIsCreateOpen(false); showToast('Đã lưu dữ liệu thành công!'); }}>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Tên khách hàng <span className="text-red-500">*</span></label>
            <input type="text" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none" placeholder="VD: Nguyễn Văn A" required />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Số điện thoại <span className="text-red-500">*</span></label>
            <div className="flex items-center">
              <input type="tel" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none" placeholder="09xxxxxxx" required />
            </div>
            <label className="flex items-center mt-2 text-sm text-slate-600">
              <input type="checkbox" className="mr-2 text-rose-600 rounded focus:ring-rose-500" defaultChecked />
              Số này có sử dụng Zalo
            </label>
          </div>
          <div className="border-t border-slate-200 pt-4 mt-4">
            <label className="block text-sm font-medium text-slate-700 mb-1">Gói dịch vụ quan tâm</label>
            <select className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none">
              <option>Chưa chọn gói</option>
              <option>Gói Kim Cương (Trọn gói)</option>
              <option>Gói Tiêu Chuẩn (Pre-wedding)</option>
              <option>Gói Lễ Ăn Hỏi</option>
            </select>
          </div>
          <button type="submit" className="w-full bg-rose-600 hover:bg-rose-700 text-white py-2 rounded-lg font-medium transition-colors mt-4">
            Lưu Dữ Liệu
          </button>
        </form>
      </Modal>

      <ContractExportModal 
        isOpen={!!exportingCustomer} 
        customer={exportingCustomer} 
        onClose={() => setExportingCustomer(null)} 
        onExport={handleExport} 
      />
    </div>
  );
};

// --- NEW FINANCIALS VIEW ---
const FinancialsView = ({ showToast }) => {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Kế Toán & Chi Phí</h2>
          <p className="text-slate-500 text-sm">Theo dõi thu chi, lợi nhuận thực tế và các khoản outsource.</p>
        </div>
        <button className="bg-rose-600 hover:bg-rose-700 text-white px-4 py-2 rounded-lg flex items-center font-medium transition-colors">
          <Plus size={18} className="mr-2" /> Ghi Chép Mới
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
         <Card className="p-6 bg-gradient-to-br from-emerald-500 to-emerald-600 text-white">
            <p className="text-emerald-100 font-medium mb-1">Tổng Thu (Tháng 6)</p>
            <h3 className="text-3xl font-bold">245.000.000đ</h3>
         </Card>
         <Card className="p-6 bg-gradient-to-br from-rose-500 to-rose-600 text-white">
            <p className="text-rose-100 font-medium mb-1">Tổng Chi Phí Thực Tế</p>
            <h3 className="text-3xl font-bold">82.500.000đ</h3>
         </Card>
         <Card className="p-6 bg-gradient-to-br from-blue-500 to-blue-600 text-white">
            <p className="text-blue-100 font-medium mb-1">Lợi Nhuận Tạm Tính</p>
            <h3 className="text-3xl font-bold">162.500.000đ</h3>
         </Card>
      </div>

      <Card className="overflow-hidden shadow-sm border-slate-200">
        <div className="p-4 border-b border-slate-100 flex justify-between items-center bg-slate-50">
          <h3 className="font-bold text-slate-900">Giao dịch gần đây</h3>
          <button className="text-sm font-medium text-rose-600">Xem tất cả</button>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-slate-500 text-xs uppercase tracking-wider">
                <th className="py-3 px-6">Ngày</th>
                <th className="py-3 px-6">Nội dung</th>
                <th className="py-3 px-6">Loại khoản</th>
                <th className="py-3 px-6 text-right">Số tiền</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
               {MOCK_TRANSACTIONS.map(trx => (
                 <tr key={trx.id} className="hover:bg-slate-50/50">
                    <td className="py-3 px-6 text-sm text-slate-600">{trx.date}</td>
                    <td className="py-3 px-6">
                      <p className="text-sm font-bold text-slate-900">{trx.desc}</p>
                      <p className="text-xs text-slate-500">{trx.id}</p>
                    </td>
                    <td className="py-3 px-6">
                      <Badge variant={trx.type === 'income' ? 'success' : 'danger'}>{trx.category}</Badge>
                    </td>
                    <td className={`py-3 px-6 text-right font-bold text-sm ${trx.type === 'income' ? 'text-emerald-600' : 'text-rose-600'}`}>
                      {trx.type === 'income' ? '+' : '-'}{trx.amount}
                    </td>
                 </tr>
               ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
};

// --- NEW SETTINGS VIEW ---
const SettingsView = ({ showToast }) => {
  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div className="mb-8">
        <h2 className="text-2xl font-bold text-slate-900">Cài Đặt Hệ Thống</h2>
        <p className="text-slate-500 text-sm">Cấu hình thông tin Studio, mẫu hợp đồng, và phân quyền.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
         {/* Settings Nav */}
         <div className="space-y-2">
           <button className="w-full text-left px-4 py-3 bg-white shadow-sm border border-slate-200 rounded-xl font-bold text-rose-600 flex items-center">
             <Building size={18} className="mr-3" /> Thông tin Studio
           </button>
           <button className="w-full text-left px-4 py-3 hover:bg-slate-100 rounded-xl font-medium text-slate-700 flex items-center transition-colors">
             <FileText size={18} className="mr-3" /> Mẫu Hợp Đồng
           </button>
           <button className="w-full text-left px-4 py-3 hover:bg-slate-100 rounded-xl font-medium text-slate-700 flex items-center transition-colors">
             <Users size={18} className="mr-3" /> Phân Quyền
           </button>
         </div>

         {/* Settings Form */}
         <Card className="p-6 md:col-span-2 space-y-5">
           <h3 className="text-lg font-bold text-slate-900 mb-4 border-b border-slate-100 pb-2">Thông tin Studio (Hiển thị trên HĐ)</h3>
           
           <div>
             <label className="block text-sm font-medium text-slate-700 mb-1">Tên Studio</label>
             <input type="text" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none" defaultValue="LUMIÈRE STUDIOS" />
           </div>
           
           <div>
             <label className="block text-sm font-medium text-slate-700 mb-1">Địa chỉ kinh doanh</label>
             <input type="text" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none" defaultValue="123 Nguyễn Văn Linh, Q7, TP.HCM" />
           </div>
           
           <div>
             <label className="block text-sm font-medium text-slate-700 mb-1">Thông tin Ngân Hàng (Nhận cọc)</label>
             <textarea rows={3} className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none" defaultValue="Vietcombank - Chi nhánh HCM&#10;STK: 0123456789&#10;Chủ TK: NGUYEN VAN A"></textarea>
           </div>
           
           <div className="pt-4 flex justify-end">
             <button onClick={() => showToast('Đã lưu cấu hình')} className="bg-slate-900 text-white px-6 py-2 rounded-lg font-medium hover:bg-slate-800 transition-colors">
               Lưu Thay Đổi
             </button>
           </div>
         </Card>
      </div>
    </div>
  )
}

export default function App() {
  const [user, setUser] = useState(null); // Auth State
  const [activeTab, setActiveTab] = useState('dashboard');
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isContractOpen, setIsContractOpen] = useState(false); 
  const [isNotifOpen, setIsNotifOpen] = useState(false); 

  // Global Search State
  const [isSearchFocused, setIsSearchFocused] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  // Smart Notifications State
  const [notifications, setNotifications] = useState([
    { id: 1, title: 'Khách hàng tới hạn thanh toán 💰', desc: 'Khách "Lê Hoàng C" tới hạn thanh toán đợt 2 (15.000.000đ). Hãy nhắn Zalo nhắc nhở.', time: '10 phút trước', read: false },
    { id: 2, title: 'Trùng lịch trang phục 👗', desc: 'Váy "Vera Wang" đang được xếp cho 2 khách vào ngày 18/10. Vui lòng kiểm tra lại.', time: '2 giờ trước', read: false },
    { id: 3, title: 'Lịch chụp ngày mai 📸', desc: 'Có 3 lịch trình vào ngày mai. Đã nhắc thợ chụp chuẩn bị thiết bị chưa?', time: '5 giờ trước', read: true },
  ]);

  // Global Toasts State
  const [toasts, setToasts] = useState([]);
  
  const showToast = (message, type = 'success') => {
    const id = Date.now();
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 4000);
  };

  const unreadNotifs = notifications.filter(n => !n.read).length;

  const handleMarkAllRead = () => {
    setNotifications(notifications.map(n => ({ ...n, read: true })));
    showToast('Đã đánh dấu tất cả thông báo là đã đọc', 'info');
  };

  const handleNotifClick = (id) => {
    setNotifications(notifications.map(n => n.id === id ? { ...n, read: true } : n));
  };

  const openGlobalTab = (tab) => {
    if (tab === 'contracts') {
      setIsContractOpen(true);
    } else {
      setActiveTab(tab);
    }
  }

  if (!user) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
        <div className="bg-white max-w-md w-full rounded-2xl shadow-xl border border-slate-200 overflow-hidden animate-in fade-in zoom-in duration-500">
          <div className="bg-rose-600 p-8 text-center">
            <div className="w-16 h-16 bg-white rounded-full flex items-center justify-center mx-auto mb-4 shadow-lg">
              <Camera size={32} className="text-rose-600" />
            </div>
            <h1 className="text-2xl font-bold text-white tracking-tight">LUMIÈRE STUDIOS</h1>
            <p className="text-rose-100 text-sm mt-1">Hệ thống Quản lý Vận hành</p>
          </div>
          <div className="p-8 space-y-4">
            <p className="text-center text-slate-500 text-sm mb-6">Vui lòng chọn vai trò đăng nhập để trải nghiệm hệ thống</p>
            <button 
              onClick={() => setUser({ role: 'admin', name: 'Quản Lý Studio' })}
              className="w-full flex items-center justify-center gap-3 bg-slate-900 hover:bg-slate-800 text-white py-3 rounded-xl font-bold transition-all hover:scale-[1.02]"
            >
              <Lock size={18} /> Đăng nhập quyền Quản Lý
            </button>
            <button 
              onClick={() => setUser({ role: 'staff', name: 'Nhân Viên' })}
              className="w-full flex items-center justify-center gap-3 bg-rose-50 hover:bg-rose-100 text-rose-700 py-3 rounded-xl font-bold transition-all hover:scale-[1.02]"
            >
              <Users size={18} /> Đăng nhập quyền Nhân Viên
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Navigation Logic (RBAC based)
  const NAVIGATION = [
    { id: 'dashboard', label: 'Tổng Quan', icon: LayoutDashboard, roles: ['admin', 'staff'] },
    { id: 'bookings', label: 'Lịch Trình', icon: CalendarDays, roles: ['admin', 'staff'] },
    { id: 'customers', label: 'Khách Hàng (CRM)', icon: HeartHandshake, roles: ['admin', 'staff'] },
    { id: 'packages', label: 'Gói Dịch Vụ', icon: Package, roles: ['admin'] },
    { id: 'assets', label: 'Kho & Thiết bị', icon: Shirt, roles: ['admin', 'staff'] },
    { id: 'staff', label: 'Nhân Sự', icon: Users, roles: ['admin'] },
    { id: 'financials', label: 'Kế Toán', icon: PieChart, roles: ['admin'] },
    { id: 'settings', label: 'Cài Đặt', icon: Settings, roles: ['admin'] },
  ];

  const allowedNavigation = NAVIGATION.filter(nav => nav.roles.includes(user.role));

  const renderContent = () => {
    switch(activeTab) {
      case 'dashboard': return <DashboardView user={user} setGlobalOpenTab={openGlobalTab} />;
      case 'bookings': return <BookingsView showToast={showToast} />;
      case 'packages': return <PackagesView showToast={showToast} />;
      case 'assets': return <AssetsView showToast={showToast} />;
      case 'staff': return <StaffView showToast={showToast} />;
      case 'customers': return <CustomersView showToast={showToast} />;
      case 'financials': return <FinancialsView showToast={showToast} />;
      case 'settings': return <SettingsView showToast={showToast} />;
      default: return <DashboardView user={user} setGlobalOpenTab={openGlobalTab} />;
    }
  };

  const handleTabChange = (id) => {
    setActiveTab(id);
    setIsMobileMenuOpen(false);
  };

  return (
    <div className="flex h-screen bg-slate-50 font-sans text-slate-900 overflow-hidden">
      
      {/* MOBILE OVERLAY */}
      {isMobileMenuOpen && (
        <div 
          className="fixed inset-0 bg-slate-900/50 z-20 md:hidden backdrop-blur-sm transition-opacity"
          onClick={() => setIsMobileMenuOpen(false)}
        />
      )}

      {/* SIDEBAR */}
      <aside className={`
        fixed inset-y-0 left-0 z-30 w-64 bg-white border-r border-slate-200 flex flex-col flex-shrink-0 transition-transform duration-300 ease-in-out
        md:static md:translate-x-0
        ${isMobileMenuOpen ? 'translate-x-0 shadow-2xl' : '-translate-x-full'}
      `}>
        <div className="p-6 flex items-center gap-3">
          <div className="bg-rose-600 text-white p-2 rounded-lg shadow-sm">
            <Camera size={24} />
          </div>
          <div>
            <h1 className="font-bold text-xl tracking-tight leading-tight">Lumière</h1>
            <p className="text-[10px] text-slate-500 uppercase tracking-widest font-medium">Studios</p>
          </div>
          <button 
            className="md:hidden ml-auto text-slate-400 hover:text-slate-600"
            onClick={() => setIsMobileMenuOpen(false)}
          >
            <X size={20} />
          </button>
        </div>
        
        <nav className="flex-1 px-4 py-2 space-y-1.5 overflow-y-auto">
          {allowedNavigation.map((item) => (
            <button
              key={item.id}
              onClick={() => handleTabChange(item.id)}
              className={`w-full flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-medium transition-all duration-200 ${
                activeTab === item.id 
                  ? 'bg-rose-50 text-rose-700 shadow-sm border border-rose-100/50' 
                  : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
              }`}
            >
              <item.icon size={18} className={activeTab === item.id ? 'text-rose-600' : 'text-slate-400'} />
              {item.label}
            </button>
          ))}
        </nav>
        
        <div className="p-4 border-t border-slate-100 bg-slate-50/50">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-rose-100 flex items-center justify-center font-bold text-rose-700 shrink-0 border border-rose-200">
              {user.name.charAt(0)}
            </div>
            <div className="flex-1 overflow-hidden">
              <p className="text-sm font-bold truncate text-slate-900">{user.name}</p>
              <p className="text-xs text-emerald-600 font-medium truncate flex items-center gap-1">
                <span className="w-1.5 h-1.5 bg-emerald-500 rounded-full"></span> Online
              </p>
            </div>
            <button onClick={() => setUser(null)} className="text-slate-400 hover:text-red-600 transition-colors p-2" title="Đăng xuất">
              <LogOut size={18} />
            </button>
          </div>
        </div>
      </aside>

      {/* MAIN CONTENT AREA */}
      <main className="flex-1 flex flex-col h-screen overflow-hidden relative">
        
        {/* HEADER */}
        <header className="bg-white/80 backdrop-blur-md border-b border-slate-200 h-16 flex items-center justify-between px-4 sm:px-6 flex-shrink-0 z-10">
          <div className="flex items-center gap-4 flex-1">
            <button 
              className="md:hidden p-2 text-slate-500 hover:bg-slate-100 rounded-lg transition-colors"
              onClick={() => setIsMobileMenuOpen(true)}
            >
              <Menu size={20} />
            </button>
            
            {/* GLOBAL SEARCH */}
            <div className="relative w-full max-w-md hidden sm:block">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
              <input 
                type="text" 
                placeholder="Tìm kiếm nhanh HĐ, SĐT khách (Ctrl+K)..." 
                className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-rose-500 focus:bg-white transition-all"
                onFocus={() => setIsSearchFocused(true)}
                onBlur={() => setTimeout(() => setIsSearchFocused(false), 200)}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
              
              {isSearchFocused && (
                <div className="absolute top-full mt-2 w-full bg-white rounded-xl shadow-xl border border-slate-200 p-2 z-50 animate-in fade-in slide-in-from-top-2">
                   <p className="text-xs font-bold text-slate-400 uppercase tracking-wider px-3 py-2">Kết quả gần đây</p>
                   <div className="p-2 hover:bg-slate-50 rounded-lg cursor-pointer flex justify-between items-center group">
                      <span className="text-sm font-medium text-slate-700 group-hover:text-rose-600">Lê Hoàng C (KH-104)</span>
                      <Badge>Khách hàng</Badge>
                   </div>
                   <div className="p-2 hover:bg-slate-50 rounded-lg cursor-pointer flex justify-between items-center group">
                      <span className="text-sm font-medium text-slate-700 group-hover:text-rose-600">HD-102 (Nguyễn Thị Lan)</span>
                      <Badge variant="brand">Hợp đồng</Badge>
                   </div>
                </div>
              )}
            </div>
          </div>
          
          <div className="flex items-center gap-3 sm:gap-4">
            {/* NOTIFICATION BELL */}
            <div className="flex items-center gap-3 relative">
              <button 
                onClick={() => setIsNotifOpen(!isNotifOpen)}
                className={`relative p-2 transition-colors rounded-lg ${isNotifOpen ? 'bg-slate-100 text-slate-900' : 'text-slate-400 hover:text-slate-600'}`}
              >
                <Bell size={20} />
                {unreadNotifs > 0 && (
                  <span className="absolute top-1.5 right-1.5 w-2.5 h-2.5 bg-rose-500 rounded-full border-2 border-white"></span>
                )}
              </button>
              
              {/* NOTIFICATION DROPDOWN */}
              {isNotifOpen && (
                <>
                  <div className="fixed inset-0 z-40" onClick={() => setIsNotifOpen(false)}></div>
                  <div className="absolute top-full right-0 mt-2 w-80 bg-white rounded-xl shadow-xl border border-slate-200 z-50 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200">
                    <div className="p-4 border-b border-slate-100 flex items-center justify-between bg-slate-50">
                      <h3 className="font-bold text-slate-900">Thông báo thông minh</h3>
                      {unreadNotifs > 0 && (
                        <span className="text-xs font-medium text-rose-600 bg-rose-50 px-2 py-1 rounded-full">{unreadNotifs} chưa đọc</span>
                      )}
                    </div>
                    <div className="max-h-[300px] overflow-y-auto">
                      {notifications.map(notif => (
                        <div 
                          key={notif.id}
                          onClick={() => handleNotifClick(notif.id)}
                          className={`p-4 border-b border-slate-50 hover:bg-slate-50 transition-colors cursor-pointer ${notif.read ? 'opacity-60' : 'opacity-100 bg-rose-50/30'}`}
                        >
                          <div className="flex justify-between items-start mb-1">
                            <p className={`text-sm ${notif.read ? 'font-medium text-slate-700' : 'font-bold text-slate-900'}`}>{notif.title}</p>
                            {!notif.read && <span className="w-2 h-2 rounded-full bg-rose-500 mt-1.5"></span>}
                          </div>
                          <p className="text-xs text-slate-500">{notif.desc}</p>
                          <p className="text-[10px] text-slate-400 mt-2">{notif.time}</p>
                        </div>
                      ))}
                    </div>
                    <div className="p-2 text-center bg-slate-50 border-t border-slate-100">
                      <button 
                        onClick={handleMarkAllRead}
                        className="text-xs font-medium text-rose-600 hover:text-rose-700 disabled:opacity-50"
                        disabled={unreadNotifs === 0}
                      >
                        Đánh dấu tất cả đã đọc
                      </button>
                    </div>
                  </div>
                </>
              )}

              {/* GLOBAL CREATE CONTRACT BUTTON */}
              {user.role === 'admin' && (
                <button 
                  onClick={() => setIsContractOpen(true)}
                  className="bg-slate-900 hover:bg-slate-800 text-white text-sm font-medium px-4 py-2 rounded-lg transition-all shadow-sm hidden sm:flex items-center gap-2"
                >
                  <FileText size={16} /> Tạo Hợp Đồng
                </button>
              )}
            </div>
          </div>
        </header>

        {/* SCROLLABLE PAGE CONTENT */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8 bg-slate-50/50">
          <div className="max-w-6xl mx-auto h-full">
            {renderContent()}
          </div>
        </div>
      </main>

      {/* GLOBAL CREATE CONTRACT MODAL */}
      <Modal isOpen={isContractOpen} onClose={() => setIsContractOpen(false)} title="Tạo Hợp Đồng Mới" width="max-w-xl">
        <form className="space-y-5" onSubmit={(e) => { 
          e.preventDefault(); 
          setIsContractOpen(false);
          showToast('Đã tạo hợp đồng thành công!', 'success');
        }}>
          
          <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 space-y-4">
            <h3 className="font-bold text-slate-900 text-sm flex items-center"><HeartHandshake className="mr-2 text-rose-600" size={16}/> Thông tin khách hàng</h3>
            <div>
              <label className="block text-xs font-medium text-slate-500 mb-1">Tìm khách hàng cũ (hoặc để trống nếu khách mới)</label>
              <input type="text" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none text-sm" placeholder="Tìm theo tên hoặc SĐT..." />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">Họ tên cô dâu / chú rể <span className="text-red-500">*</span></label>
                <input type="text" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none text-sm" required />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">Số Zalo / Điện thoại <span className="text-red-500">*</span></label>
                <input type="tel" className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none text-sm" required />
              </div>
            </div>
          </div>

          <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 space-y-4">
            <h3 className="font-bold text-slate-900 text-sm flex items-center"><Package className="mr-2 text-rose-600" size={16}/> Chi tiết dịch vụ</h3>
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">Chọn gói dịch vụ <span className="text-red-500">*</span></label>
              <select className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none text-sm">
                {MOCK_PACKAGES.map(p => <option key={p.id}>{p.name} - {p.price}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-700 mb-1">Ghi chú thêm (dịch vụ phát sinh)</label>
              <textarea rows={2} className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-rose-500 outline-none text-sm"></textarea>
            </div>
          </div>

          <div className="bg-rose-50/50 p-4 rounded-xl border border-rose-100 space-y-4">
            <h3 className="font-bold text-rose-900 text-sm flex items-center"><DollarSign className="mr-2 text-rose-600" size={16}/> Thanh toán</h3>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">Tổng giá trị hợp đồng <span className="text-red-500">*</span></label>
                <input type="text" className="w-full px-4 py-2 border border-slate-300 rounded-lg bg-white outline-none text-sm font-bold text-slate-900" defaultValue="25.000.000đ" required />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">Tiền cọc đợt 1 <span className="text-red-500">*</span></label>
                <input type="text" className="w-full px-4 py-2 border border-rose-300 rounded-lg bg-white focus:ring-2 focus:ring-rose-500 outline-none text-sm font-bold text-rose-700" placeholder="VD: 5.000.000đ" required />
              </div>
            </div>
          </div>

          <button type="submit" className="w-full bg-rose-600 hover:bg-rose-700 text-white py-3 rounded-xl font-bold transition-colors mt-6 shadow-md shadow-rose-200">
            Lưu & Tạo Hợp Đồng
          </button>
        </form>
      </Modal>

      {/* TOAST CONTAINER */}
      <div className="fixed bottom-4 right-4 z-[70] flex flex-col gap-2">
        {toasts.map(toast => (
          <div key={toast.id} className="animate-in slide-in-from-right-5 fade-in duration-300">
            <div className={`flex items-center gap-3 px-4 py-3 rounded-lg shadow-lg border bg-white ${
              toast.type === 'info' ? 'border-blue-200 text-blue-800' : 'border-emerald-200 text-emerald-800'
            }`}>
              {toast.type === 'info' ? <AlertCircle size={20} className="text-blue-500" /> : <CheckCircle2 size={20} className="text-emerald-500" />}
              <p className="text-sm font-medium">{toast.message}</p>
            </div>
          </div>
        ))}
      </div>

    </div>
  );
}