import React, { useState, useEffect, useRef } from 'react';
import { 
  ArrowLeft, Trash2, Plus, CheckCircle2, Calendar, 
  List, Home, Camera, Sparkles, User, Mail, Lock, Search, 
  ChevronRight, Menu, Bell, Send, MessageSquare, Heart,
  Star, MapPin, LogOut, Settings, CreditCard, BellRing, Shield, Image as ImageIcon,
  Users, Share2, Link as LinkIcon, Briefcase, TrendingUp, CalendarDays, MoreHorizontal,
  UserPlus, FileText, DollarSign, Filter, Phone, Edit, Save
} from 'lucide-react';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState('splash');
  const [isLoading, setIsLoading] = useState(false);
  const [userData, setUserData] = useState({
    name: 'Ngọc & Hoàng',
    email: 'ngoc.hoang@example.com',
    role: null, // 'couple' or 'vendor'
    hasPlan: false,
  });
  const [justCreated, setJustCreated] = useState(false);
  const [selectedService, setSelectedService] = useState(null);

  const mockServices = [
    { 
      id: 1, name: 'Chụp ảnh Pre-Wedding', provider: 'Lumiere Studio', price: '15.000.000 ₫', rating: 4.9, reviews: 128, location: 'Quận 1, TP.HCM',
      icon: <Camera size={20}/>, color: 'text-blue-500', bg: 'bg-blue-50',
      image: 'https://images.unsplash.com/photo-1511285560929-80b456fea0bc?q=80&w=1000&auto=format&fit=crop',
      desc: 'Gói chụp ảnh ngoại cảnh 2 ngày 1 đêm tại Đà Lạt hoặc các điểm lân cận TP.HCM. Bao gồm 3 váy cưới, 2 vest, trang điểm đi kèm và 1 album photobook cao cấp.'
    },
    { 
      id: 2, name: 'Trang trí tiệc cưới', provider: 'Dreamy Decor', price: '25.000.000 ₫', rating: 4.8, reviews: 85, location: 'Quận Phú Nhuận, TP.HCM',
      icon: <Sparkles size={20}/>, color: 'text-amber-500', bg: 'bg-amber-50',
      image: 'https://images.unsplash.com/photo-1519225421980-715cb0215aed?q=80&w=1000&auto=format&fit=crop',
      desc: 'Trang trí trọn gói với concept hoa tươi 100%. Bao gồm cổng hoa, bàn gallery, lối đi sân khấu và backdrop chụp hình chuẩn phong cách hiện đại.'
    }
  ];

  const simulateLoading = (nextScreen, delay = 800) => {
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
      setCurrentScreen(nextScreen);
    }, delay);
  };

  useEffect(() => {
    if (currentScreen === 'splash') {
      const timer = setTimeout(() => setCurrentScreen('login'), 2000);
      return () => clearTimeout(timer);
    }
  }, [currentScreen]);

  const showBottomNav = ['home', 'plan_management', 'search', 'profile', 'vendor_projects', 'vendor_messages'].includes(currentScreen);

  const ScreenSplash = () => (
    <div className="flex flex-col h-full items-center justify-center bg-gradient-to-br from-pink-500 to-rose-400 text-white">
      <Heart size={64} className="mb-4 animate-pulse fill-white" />
      <h1 className="text-4xl font-bold tracking-wider">WedPlanner</h1>
      <p className="mt-2 text-pink-100 font-medium">Hành trình hạnh phúc của bạn</p>
    </div>
  );

  const ScreenLogin = () => (
    <div className="h-full flex flex-col p-6 bg-white overflow-y-auto pb-10 animate-in fade-in">
      <div className="mt-12 mb-8 space-y-2">
        <h2 className="text-3xl font-bold text-slate-800">Đăng nhập</h2>
        <p className="text-slate-500">Chào mừng bạn đến với WedPlanner!</p>
      </div>
      <div className="space-y-4 mb-6">
        <div>
          <label className="block text-sm font-semibold text-slate-700 mb-1">Email</label>
          <div className="relative">
            <Mail className="absolute left-3 top-3 text-slate-400" size={20} />
            <input type="email" placeholder="Nhập email của bạn" className="w-full pl-10 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-pink-500 outline-none" />
          </div>
        </div>
        <div>
          <label className="block text-sm font-semibold text-slate-700 mb-1">Mật khẩu</label>
          <div className="relative">
            <Lock className="absolute left-3 top-3 text-slate-400" size={20} />
            <input type="password" placeholder="••••••••" className="w-full pl-10 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-pink-500 outline-none" />
          </div>
        </div>
      </div>
      <button onClick={() => simulateLoading('otp')} className="w-full py-4 bg-pink-600 text-white rounded-xl font-bold text-lg mb-4 hover:bg-pink-700 active:scale-95 flex justify-center transition-all shadow-lg shadow-pink-200">
        {isLoading ? <div className="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin" /> : 'Đăng nhập'}
      </button>
      
      <div className="text-center text-slate-500 text-sm mb-4">hoặc</div>

      <button onClick={() => simulateLoading('role_select')} className="w-full py-3.5 bg-white border border-slate-200 text-slate-700 rounded-xl font-bold flex items-center justify-center gap-3 hover:bg-slate-50 active:scale-95 transition-all shadow-sm">
        <img src="https://www.svgrepo.com/show/475656/google-color.svg" alt="Google" className="w-5 h-5" />
        Tiếp tục với Google
      </button>
    </div>
  );

  const ScreenOTP = () => (
    <div className="h-full flex flex-col p-6 bg-white pb-10 animate-in slide-in-from-right-4">
      <button onClick={() => setCurrentScreen('login')} className="mb-6 mt-4 w-fit text-slate-500"><ArrowLeft size={24} /></button>
      <h2 className="text-2xl font-bold text-slate-800 mb-2">Xác thực tài khoản</h2>
      <p className="text-slate-500 mb-8">Chúng tôi đã gửi mã 4 số tới email của bạn.</p>
      <div className="flex justify-between gap-4 mb-8">
        {[1, 2, 3, 4].map((i) => (<input key={i} type="text" maxLength="1" className="w-16 h-16 text-center text-2xl font-bold bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-pink-500 outline-none" defaultValue={i === 1 ? '8' : ''} />))}
      </div>
      <button onClick={() => simulateLoading('role_select')} className="w-full py-4 bg-pink-600 text-white rounded-xl font-bold text-lg flex justify-center hover:bg-pink-700">
        {isLoading ? <div className="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin" /> : 'Xác nhận'}
      </button>
    </div>
  );

  const ScreenRoleSelect = () => (
    <div className="h-full flex flex-col p-6 bg-white justify-center pb-10 animate-in zoom-in-95">
      <div className="text-center mb-10">
        <h2 className="text-2xl font-bold text-slate-800 mb-2">Bạn là ai?</h2>
        <p className="text-slate-500">Hãy cho chúng tôi biết để cá nhân hóa trải nghiệm.</p>
      </div>
      <div className="space-y-4">
        <button onClick={() => { setUserData({...userData, role: 'couple', name: 'Ngọc & Hoàng', hasPlan: false}); simulateLoading('home'); }} className="w-full p-6 text-left border-2 border-slate-100 rounded-2xl hover:border-pink-500 focus:bg-pink-50 flex items-center justify-between">
          <div><h3 className="font-bold text-slate-800 text-lg">Cô Dâu / Chú Rể</h3><p className="text-sm text-slate-500 mt-1">Đang lên kế hoạch cho đám cưới</p></div><Heart className="text-pink-400" size={32} />
        </button>
        <button onClick={() => { setUserData({...userData, role: 'vendor', name: 'Lumiere Studio', hasPlan: true}); simulateLoading('home'); }} className="w-full p-6 text-left border-2 border-slate-100 rounded-2xl hover:border-slate-800 focus:bg-slate-50 flex items-center justify-between">
          <div><h3 className="font-bold text-slate-800 text-lg">Freelancer / Dịch vụ</h3><p className="text-sm text-slate-500 mt-1">Nhiếp ảnh gia, Trang điểm...</p></div><Briefcase className="text-slate-400" size={32} />
        </button>
      </div>
    </div>
  );

  const ScreenHome = () => {
    if (userData.role === 'vendor') {
      return (
        <div className="h-full flex flex-col bg-slate-50 animate-in fade-in">
          <div className="bg-slate-900 p-6 pt-12 pb-8 rounded-b-[2.5rem] relative text-white">
            <div className="flex justify-between items-center mb-6">
              <div><p className="text-slate-400 text-sm font-medium">Dashboard</p><h1 className="text-2xl font-bold">{userData.name}</h1></div>
              <button onClick={() => setCurrentScreen('notifications')} className="p-2 bg-slate-800 rounded-full relative"><Bell size={22} /><span className="absolute top-1.5 right-1.5 w-2 h-2 bg-pink-500 rounded-full"></span></button>
            </div>
            <div className="grid grid-cols-3 gap-3">
              <div className="bg-slate-800 p-3 rounded-2xl"><p className="text-xs text-slate-400 mb-1">Doanh thu</p><p className="text-lg font-bold">120tr</p></div>
              <div className="bg-slate-800 p-3 rounded-2xl"><p className="text-xs text-slate-400 mb-1">Yêu cầu</p><p className="text-lg font-bold text-pink-400">3 mới</p></div>
              <div className="bg-slate-800 p-3 rounded-2xl"><p className="text-xs text-slate-400 mb-1">Đánh giá</p><p className="text-lg font-bold text-amber-400 flex items-center gap-1">4.9 <Star size={14} className="fill-amber-400"/></p></div>
            </div>
          </div>
          <div className="px-6 flex-1 overflow-y-auto pt-6 pb-24 space-y-6">
            <div>
              <div className="flex justify-between items-end mb-4"><h3 className="font-bold text-slate-800 text-lg">Yêu cầu báo giá mới</h3><span className="text-pink-600 text-sm font-semibold">Tất cả</span></div>
              <div className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex items-center justify-between mb-3">
                <div className="flex items-center gap-3"><div className="w-10 h-10 rounded-full bg-slate-100 flex items-center justify-center text-slate-500"><User size={20}/></div><div><h4 className="font-semibold text-slate-800">Trần Vy</h4><p className="text-xs text-slate-500">Gói Chụp Pre-Wedding</p></div></div>
                <button className="px-3 py-1.5 bg-pink-50 text-pink-600 text-xs font-bold rounded-lg hover:bg-pink-100">Phản hồi</button>
              </div>
            </div>
            <div>
              <h3 className="font-bold text-slate-800 text-lg mb-4">Lịch trình sắp tới</h3>
              <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100 flex gap-4 items-center">
                <div className="w-14 h-14 rounded-xl bg-blue-50 text-blue-600 flex flex-col items-center justify-center shrink-0"><span className="text-xs font-bold">TH 10</span><span className="text-xl font-bold leading-none">20</span></div>
                <div><h4 className="font-bold text-slate-800">Lễ Cưới Ngọc & Hoàng</h4><p className="text-sm text-slate-500 mt-1 flex items-center gap-1"><MapPin size={14}/> Gem Center, Quận 1</p></div>
              </div>
            </div>
          </div>
        </div>
      );
    }
    return (
      <div className="h-full flex flex-col bg-slate-50 animate-in fade-in">
        <div className="bg-white p-6 pt-12 pb-6 rounded-b-[2rem] shadow-sm mb-6 flex justify-between items-center relative z-10">
          <div><p className="text-slate-500 text-sm font-medium">Xin chào,</p><h1 className="text-2xl font-bold text-slate-800">{userData.name}</h1></div>
          <button onClick={() => setCurrentScreen('notifications')} className="p-2 bg-slate-50 rounded-full relative"><Bell size={22} /><span className="absolute top-1.5 right-1.5 w-2 h-2 bg-pink-500 rounded-full"></span></button>
        </div>
        <div className="px-6 flex-1 overflow-y-auto pb-24 space-y-6">
          {!userData.hasPlan ? (
            <div className="bg-gradient-to-r from-pink-500 to-rose-400 rounded-3xl p-6 text-white shadow-lg relative overflow-hidden">
              <div className="relative z-10"><h2 className="text-xl font-bold mb-2">Bắt đầu hành trình</h2><p className="text-pink-100 text-sm mb-4 max-w-[80%]">Lên kế hoạch hoàn hảo cho ngày trọng đại ngay hôm nay.</p><button onClick={() => setCurrentScreen('create_plan')} className="bg-white text-pink-600 px-5 py-2.5 rounded-xl font-bold text-sm">Bắt đầu ngay</button></div>
              <Heart size={100} className="absolute -bottom-4 -right-4 text-white opacity-10" fill="currentColor"/>
            </div>
          ) : (
            <div className="bg-gradient-to-r from-emerald-400 to-teal-500 rounded-3xl p-6 text-white shadow-lg">
              <div className="flex justify-between items-center"><div><p className="text-emerald-100 text-sm mb-1">Ngày cưới (Dự kiến)</p><h2 className="text-2xl font-bold">20 Tháng 10, 2026</h2></div><div className="bg-white/20 p-3 rounded-2xl"><Calendar size={28} /></div></div>
              <div className="mt-4 pt-4 border-t border-white/20 flex justify-between items-center"><span className="text-sm">Đã hoàn thành</span><span className="font-bold">45%</span></div>
            </div>
          )}
          <div className="grid grid-cols-4 gap-3">
            {[ { id: userData.hasPlan ? 'plan_management' : 'create_plan', label: userData.hasPlan ? 'Kế hoạch' : 'Tạo mới', icon: userData.hasPlan ? <List size={22} /> : <CheckCircle2 size={22} />, color: userData.hasPlan ? 'text-blue-500 bg-blue-50' : 'text-emerald-500 bg-emerald-50' },
               { id: 'ai_chat', label: 'AI Chat', icon: <Sparkles size={22} />, color: 'text-purple-500 bg-purple-50' },
               { id: 'guest_list', label: 'Khách mời', icon: <Users size={22} />, color: 'text-orange-500 bg-orange-50' },
               { id: 'share_plan', label: 'Đồng bộ', icon: <Share2 size={22} />, color: 'text-pink-500 bg-pink-50' } ].map((act, idx) => (
              <button key={idx} onClick={() => simulateLoading(act.id, 500)} className="bg-white p-3 rounded-2xl shadow-sm border border-slate-100 flex flex-col items-center justify-center gap-2 hover:bg-slate-50"><div className={`w-12 h-12 rounded-full flex items-center justify-center ${act.color}`}>{act.icon}</div><span className="font-semibold text-slate-700 text-[10px] uppercase">{act.label}</span></button>
            ))}
          </div>
          <div>
            <div className="flex justify-between items-end mb-4 mt-2"><h3 className="font-bold text-slate-800 text-lg">Dịch vụ đề xuất</h3><button onClick={() => setCurrentScreen('search')} className="text-pink-600 text-sm font-semibold hover:underline">Xem tất cả</button></div>
            <div className="space-y-3">
              {mockServices.map(svc => (
                  <button key={svc.id} onClick={() => { setSelectedService(svc); setCurrentScreen('service_detail'); }} className="w-full bg-white p-4 rounded-2xl flex items-center justify-between shadow-sm border border-slate-100 text-left hover:border-pink-200">
                    <div className="flex items-center gap-4"><div className={`w-12 h-12 rounded-xl flex items-center justify-center ${svc.color} ${svc.bg} shrink-0`}>{svc.icon}</div><div><h4 className="font-semibold text-slate-800">{svc.name}</h4><p className="text-xs text-slate-500 mt-0.5">{svc.provider} • <Star size={10} className="inline fill-amber-400 text-amber-400"/> {svc.rating}</p></div></div><ChevronRight className="text-slate-400 shrink-0" size={20}/>
                  </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  };

  const ScreenServiceDetail = () => (
    <div className="flex flex-col h-full bg-slate-50 relative pb-20 overflow-y-auto animate-in slide-in-from-right-4">
      <div className="relative h-64 shrink-0">
        <img src={selectedService?.image} alt={selectedService?.name} className="w-full h-full object-cover" />
        <button onClick={() => setCurrentScreen('home')} className="absolute top-12 left-4 w-10 h-10 bg-white/80 backdrop-blur-sm rounded-full flex items-center justify-center text-slate-800 shadow-sm"><ArrowLeft size={20}/></button>
      </div>
      <div className="bg-white -mt-6 rounded-t-[2rem] relative z-10 p-6 space-y-4 shadow-sm border-t border-slate-100 min-h-screen">
         <div className="flex justify-between items-start">
           <div><h2 className="text-2xl font-bold text-slate-800">{selectedService?.name}</h2><p className="text-slate-500 font-medium mt-1">{selectedService?.provider} • {selectedService?.location}</p></div>
           <div className="flex items-center gap-1 bg-amber-50 px-2 py-1 rounded-lg shrink-0"><Star size={14} className="text-amber-500 fill-amber-500"/><span className="font-bold text-amber-600">{selectedService?.rating}</span></div>
         </div>
         <p className="text-2xl font-bold text-pink-600 border-b border-slate-100 pb-4">{selectedService?.price}</p>
         <div>
            <h4 className="font-bold text-slate-800 mb-2">Mô tả dịch vụ</h4>
            <p className="text-slate-600 text-sm leading-relaxed">{selectedService?.desc}</p>
         </div>
      </div>
      <div className="fixed bottom-0 w-full max-w-[400px] bg-white p-4 border-t border-slate-100 z-50 shadow-[0_-10px_20px_rgba(0,0,0,0.05)]">
         <button className="w-full bg-pink-600 text-white py-4 rounded-xl font-bold text-lg shadow-lg shadow-pink-200 hover:bg-pink-700 active:scale-95 transition-all">Liên hệ đặt lịch</button>
      </div>
    </div>
  );

  const ScreenCreatePlan = () => {
    const [categories, setCategories] = useState([
      { id: 1, name: 'Nhà hàng & Tiệc', amount: '150000000' },
      { id: 2, name: 'Chụp ảnh & Quay phim', amount: '30000000' }
    ]);
    const updateCategory = (id, field, value) => setCategories(categories.map(c => c.id === id ? { ...c, [field]: value } : c));
    return (
      <div className="flex flex-col h-full bg-slate-50 relative pb-20 animate-in slide-in-from-right-4">
        <div className="bg-white px-6 pt-12 pb-4 shadow-sm flex items-center sticky top-0 z-10"><button onClick={() => setCurrentScreen('home')} className="mr-4 text-slate-500"><ArrowLeft size={24}/></button><h2 className="text-xl font-bold text-slate-800">Tạo Kế Hoạch</h2></div>
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100 space-y-5">
            <div><label className="block text-sm font-semibold text-slate-700 mb-1">Tổng ngân sách</label><input type="number" placeholder="VD: 300.000.000" className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-xl text-lg font-bold text-pink-600 outline-none" /></div>
            <div className="pt-2"><label className="block text-sm font-semibold text-slate-700 mb-3">Danh mục chi phí</label>
              <div className="space-y-3">
                {categories.map((cat) => (
                  <div key={cat.id} className="grid grid-cols-[1fr,auto] gap-3 items-center bg-slate-50 p-3 rounded-xl border border-slate-200">
                    <div className="flex flex-col gap-1.5">
                      <input value={cat.name} onChange={(e) => updateCategory(cat.id, 'name', e.target.value)} placeholder="Tên danh mục..." className="w-full bg-transparent text-sm font-semibold text-slate-800 outline-none" />
                      <div className="flex items-center text-xs text-slate-500"><span className="mr-2">Giá:</span><input type="number" value={cat.amount} onChange={(e) => updateCategory(cat.id, 'amount', e.target.value)} className="w-24 bg-transparent text-pink-600 font-bold outline-none" />đ</div>
                    </div>
                    <button onClick={() => setCategories(categories.filter(c => c.id !== cat.id))} className="w-10 h-10 text-slate-400 hover:text-red-500 rounded-lg flex items-center justify-center"><Trash2 size={18} /></button>
                  </div>
                ))}
              </div>
              <button onClick={() => setCategories([...categories, {id: Date.now(), name: '', amount: ''}])} className="mt-4 w-full py-3.5 border-2 border-dashed border-pink-200 bg-pink-50/50 rounded-xl text-pink-600 font-semibold text-sm flex justify-center gap-2"><Plus size={18} /> Thêm danh mục</button>
            </div>
          </div>
        </div>
        <div className="absolute bottom-0 w-full bg-white p-4 border-t z-10"><button onClick={() => { setUserData({...userData, hasPlan: true}); setJustCreated(true); simulateLoading('plan_management'); }} className="w-full bg-pink-600 text-white py-4 rounded-xl font-bold">Xác nhận tạo Kế Hoạch</button></div>
      </div>
    );
  };

  const ScreenAIChat = () => {
    const [messages, setMessages] = useState([{ id: 1, sender: 'ai', text: 'Chào bạn! Mình là AI WedPlanner. Ngân sách dự kiến cho đám cưới của bạn là bao nhiêu?' }]);
    const [inputText, setInputText] = useState('');
    const handleSend = () => {
      if(!inputText) return;
      setMessages([...messages, { id: Date.now(), sender: 'user', text: inputText }]);
      setInputText('');
      setTimeout(() => setMessages(prev => [...prev, { id: Date.now()+1, sender: 'ai', text: 'Mình đã ghi nhận. Hãy nhấn nút bên dưới để tạo kế hoạch tự động nhé!' }]), 1000);
    };
    return (
      <div className="flex flex-col h-full bg-slate-50 relative pb-20 animate-in slide-in-from-right-4">
        <div className="bg-white px-6 pt-12 pb-4 shadow-sm flex items-center sticky top-0 z-10"><button onClick={() => setCurrentScreen('home')} className="mr-4 text-slate-500"><ArrowLeft size={24}/></button><div><h2 className="text-xl font-bold text-slate-800">Trợ lý AI</h2><p className="text-xs text-green-500 font-medium">Đang hoạt động</p></div></div>
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {messages.map(msg => (
            <div key={msg.id} className={`flex ${msg.sender === 'user' ? 'justify-end' : 'justify-start'}`}><div className={`max-w-[80%] p-3 rounded-2xl text-sm ${msg.sender === 'user' ? 'bg-pink-600 text-white rounded-br-none' : 'bg-white border text-slate-800 rounded-bl-none'}`}>{msg.text}</div></div>
          ))}
          {messages.length > 2 && <div className="flex justify-center mt-6"><button onClick={() => {setUserData({...userData, hasPlan: true}); simulateLoading('plan_management');}} className="bg-gradient-to-r from-pink-500 to-purple-600 text-white px-6 py-3 rounded-xl font-bold shadow-lg flex items-center gap-2"><Sparkles size={18}/> Sinh Kế Hoạch</button></div>}
        </div>
        <div className="absolute bottom-0 w-full p-4 bg-white border-t flex gap-2"><input value={inputText} onChange={e=>setInputText(e.target.value)} onKeyPress={e=>e.key==='Enter'&&handleSend()} placeholder="Nhập câu trả lời..." className="flex-1 bg-slate-100 rounded-full px-4 py-3 text-sm outline-none" /><button onClick={handleSend} className="w-12 h-12 bg-pink-600 text-white rounded-full flex items-center justify-center shrink-0"><Send size={18}/></button></div>
      </div>
    );
  };

  const ScreenPlanManagement = () => {
    const [activeTab, setActiveTab] = useState('overview');

    return (
      <div className="flex flex-col h-full bg-slate-50 relative pb-20 animate-in fade-in">
        <div className="bg-white px-6 pt-12 pb-2 shadow-sm sticky top-0 z-10">
          <div className="flex items-center mb-4"><button onClick={() => setCurrentScreen('home')} className="mr-4 text-slate-500"><ArrowLeft size={24}/></button><h2 className="text-xl font-bold text-slate-800">Chi tiết kế hoạch</h2></div>
          <div className="flex space-x-6 border-b border-slate-100">
            {['overview', 'tasks', 'budget'].map((tab) => (
              <button key={tab} onClick={() => setActiveTab(tab)} className={`pb-3 text-sm font-semibold transition-all relative capitalize ${activeTab === tab ? 'text-pink-600' : 'text-slate-400'}`}>
                {tab === 'overview' ? 'Tổng quan' : tab === 'tasks' ? 'Công việc (4)' : 'Ngân sách'}
                {activeTab === tab && <div className="absolute bottom-0 left-0 w-full h-0.5 bg-pink-600 rounded-t-full"></div>}
              </button>
            ))}
          </div>
        </div>
        <div className="flex-1 overflow-y-auto p-6">
           {activeTab === 'overview' && (
             <div className="space-y-4 animate-in fade-in duration-300">
               <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100">
                 <h4 className="font-bold text-slate-700 flex items-center gap-2 mb-4"><Calendar size={18} className="text-pink-500"/> Ngày trọng đại</h4>
                 <div className="flex justify-between items-center bg-slate-50 p-4 rounded-xl">
                   <div className="text-center"><p className="text-xs text-slate-500 mb-1">Đám hỏi</p><p className="font-bold text-slate-800">15/10/2026</p></div>
                   <div className="w-px h-8 bg-slate-200"></div>
                   <div className="text-center"><p className="text-xs text-slate-500 mb-1">Lễ Cưới</p><p className="font-bold text-pink-600">20/10/2026</p></div>
                 </div>
               </div>
               <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100">
                 <h4 className="font-bold text-slate-700 mb-1 flex items-center gap-2"><List size={18} className="text-blue-500"/> Tóm tắt ngân sách</h4>
                 <p className="text-3xl font-bold text-slate-800 mt-2">300.000.000 ₫</p>
                 <div className="w-full bg-slate-100 h-2 rounded-full mt-4 overflow-hidden"><div className="bg-pink-500 h-full w-[45%] rounded-full"></div></div>
                 <p className="text-xs text-slate-500 mt-2">Đã chi tiêu: 45% (135tr)</p>
               </div>
             </div>
           )}
           {activeTab === 'tasks' && (
             <div className="space-y-4 animate-in fade-in duration-300">
                <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100">
                  <h4 className="font-bold text-slate-700 mb-4">Tháng này</h4>
                  <ul className="space-y-4">
                    <li className="flex items-start gap-3"><input type="checkbox" className="mt-1 w-5 h-5 rounded border-slate-300 text-pink-600" /> <div><p className="font-semibold text-slate-700">Khảo sát địa điểm</p><p className="text-xs text-slate-500">Hạn chót: 15/05</p></div></li>
                    <li className="flex items-start gap-3"><input type="checkbox" className="mt-1 w-5 h-5 rounded border-slate-300 text-pink-600" /> <div><p className="font-semibold text-slate-700">Lên danh sách khách mời</p><p className="text-xs text-slate-500">Hạn chót: 30/05</p></div></li>
                  </ul>
                </div>
             </div>
           )}
           {activeTab === 'budget' && (
             <div className="space-y-4 animate-in fade-in duration-300">
                <div className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100 space-y-4">
                  <div className="flex justify-between items-center pb-4 border-b border-slate-100">
                     <div><p className="text-sm text-slate-500">Tổng ngân sách</p><p className="font-bold text-slate-800 text-lg">300.000.000 ₫</p></div>
                     <button className="text-pink-600 bg-pink-50 p-2 rounded-lg hover:bg-pink-100"><Plus size={20}/></button>
                  </div>
                  <div className="space-y-4 pt-2">
                    <div className="flex justify-between items-center">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-orange-50 text-orange-500 flex items-center justify-center"><Home size={18}/></div>
                        <div><p className="font-semibold text-slate-700 text-sm">Nhà hàng</p><p className="text-xs text-slate-500">Đã đặt cọc</p></div>
                      </div>
                      <span className="font-bold text-slate-700">150tr ₫</span>
                    </div>
                    <div className="flex justify-between items-center">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-500 flex items-center justify-center"><Camera size={18}/></div>
                        <div><p className="font-semibold text-slate-700 text-sm">Chụp ảnh</p><p className="text-xs text-slate-500">Chưa chốt</p></div>
                      </div>
                      <span className="font-bold text-slate-700">30tr ₫</span>
                    </div>
                  </div>
                </div>
             </div>
           )}
        </div>
      </div>
    );
  };

  const ScreenNotifications = () => (
    <div className="flex flex-col h-full bg-slate-50 relative pb-20 animate-in slide-in-from-bottom-4">
      <div className="bg-white px-6 pt-12 pb-4 shadow-sm flex items-center sticky top-0 z-10"><button onClick={() => setCurrentScreen('home')} className="mr-4 text-slate-500"><ArrowLeft size={24}/></button><h2 className="text-xl font-bold text-slate-800">Thông báo</h2></div>
      <div className="p-4 space-y-3">
         <div className="p-4 rounded-2xl flex gap-4 items-start bg-white shadow-sm border border-pink-100"><div className="w-12 h-12 rounded-full flex items-center justify-center shrink-0 bg-purple-100 text-purple-500"><Sparkles size={20}/></div><div><h4 className="text-sm font-bold text-slate-800">Kế hoạch đã sẵn sàng</h4><p className="text-xs text-slate-500 mt-1">Trợ lý AI đã hoàn tất bản nháp kế hoạch cưới của bạn.</p></div></div>
         <div className="p-4 rounded-2xl flex gap-4 items-start bg-transparent border border-slate-100"><div className="w-12 h-12 rounded-full flex items-center justify-center shrink-0 bg-blue-100 text-blue-500"><MessageSquare size={20}/></div><div><h4 className="text-sm font-semibold text-slate-600">Lumiere Studio</h4><p className="text-xs text-slate-500 mt-1">Nhà cung cấp đã phản hồi tin nhắn của bạn.</p></div></div>
      </div>
    </div>
  );

  const ScreenVendorProjects = () => (
    <div className="h-full flex flex-col bg-slate-50 animate-in fade-in pb-20">
      <div className="bg-white p-6 pt-12 pb-4 shadow-sm border-b border-slate-100 flex justify-between items-center">
        <h2 className="text-2xl font-bold text-slate-800">Dự án của tôi</h2>
        <button className="p-2 bg-slate-100 rounded-xl"><Filter size={20} className="text-slate-600"/></button>
      </div>
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {[
          { name: 'Đám cưới Ngọc & Hoàng', date: '20/10/2026', status: 'Sắp tới', color: 'text-blue-600 bg-blue-50 border-blue-100', service: 'Chụp Phóng sự' },
          { name: 'Lễ cưới Lan & Tuấn', date: '15/09/2026', status: 'Đang làm', color: 'text-amber-600 bg-amber-50 border-amber-100', service: 'Chụp Pre-Wedding' },
          { name: 'Tiệc cưới Minh & Vy', date: '01/08/2026', status: 'Hoàn thành', color: 'text-emerald-600 bg-emerald-50 border-emerald-100', service: 'Chụp Phóng sự' },
        ].map((proj, i) => (
          <div key={i} className="bg-white p-5 rounded-2xl shadow-sm border border-slate-100 relative overflow-hidden group">
             <div className="flex justify-between items-start mb-3">
               <div>
                 <h4 className="font-bold text-slate-800 text-lg">{proj.name}</h4>
                 <p className="text-sm text-slate-500 mt-1">{proj.service}</p>
               </div>
               <span className={`text-[10px] font-bold px-2.5 py-1 rounded-lg border ${proj.color}`}>{proj.status}</span>
             </div>
             <div className="flex items-center gap-2 text-sm font-medium text-slate-600 bg-slate-50 w-fit px-3 py-1.5 rounded-lg"><Calendar size={14} className="text-slate-400"/> {proj.date}</div>
          </div>
        ))}
      </div>
    </div>
  );

  const ScreenVendorMessages = () => (
    <div className="h-full flex flex-col bg-slate-50 animate-in fade-in pb-20">
      <div className="bg-white p-6 pt-12 pb-4 shadow-sm border-b border-slate-100">
        <h2 className="text-2xl font-bold text-slate-800">Tin nhắn</h2>
      </div>
      <div className="flex-1 overflow-y-auto">
        {[
          { name: 'Trần Vy', lastMsg: 'Anh ơi, em muốn đổi concept chụp sang phong cách Vintage được không ạ?', time: '10:30', unread: 2 },
          { name: 'Lan & Tuấn', lastMsg: 'Cảm ơn anh vì bộ ảnh cưới quá đẹp!', time: 'Hôm qua', unread: 0 },
        ].map((msg, i) => (
          <div key={i} className={`flex items-center gap-4 p-4 border-b border-slate-100 bg-white cursor-pointer hover:bg-slate-50 ${msg.unread > 0 ? 'bg-pink-50/20' : ''}`}>
            <div className="w-14 h-14 bg-gradient-to-tr from-slate-200 to-slate-100 rounded-full flex items-center justify-center font-bold text-slate-500 text-xl shrink-0">
              {msg.name.charAt(0)}
            </div>
            <div className="flex-1 overflow-hidden">
              <div className="flex justify-between items-center mb-1">
                <h4 className={`font-bold text-slate-800 ${msg.unread > 0 ? 'text-[15px]' : 'text-sm'}`}>{msg.name}</h4>
                <span className="text-[10px] text-slate-400 font-medium">{msg.time}</span>
              </div>
              <p className={`text-xs truncate ${msg.unread > 0 ? 'text-slate-800 font-semibold' : 'text-slate-500'}`}>{msg.lastMsg}</p>
            </div>
            {msg.unread > 0 && <span className="w-6 h-6 bg-pink-500 text-white rounded-full text-xs flex items-center justify-center font-bold shrink-0">{msg.unread}</span>}
          </div>
        ))}
      </div>
    </div>
  );

  const ScreenProfile = () => {
    const isVendor = userData.role === 'vendor';
    return (
      <div className="h-full flex flex-col bg-slate-50 animate-in fade-in">
        <div className={`px-6 pt-12 pb-6 shadow-sm rounded-b-[2rem] relative z-10 text-center ${isVendor ? 'bg-slate-900 text-white' : 'bg-white'}`}>
          <div className={`w-24 h-24 rounded-full mx-auto mb-4 p-1 shadow-lg ${isVendor ? 'bg-slate-700' : 'bg-gradient-to-tr from-pink-400 to-rose-400'}`}>
            <div className={`w-full h-full rounded-full flex items-center justify-center border-2 ${isVendor ? 'bg-slate-800 border-slate-700 text-slate-300' : 'bg-white border-white text-pink-300'}`}>
              <User size={40} />
            </div>
          </div>
          <h2 className={`text-2xl font-bold ${isVendor ? 'text-white' : 'text-slate-800'}`}>{userData.name}</h2>
          <p className={`text-sm mt-1 ${isVendor ? 'text-slate-400' : 'text-slate-500'}`}>{userData.email}</p>
          <div className="flex justify-center gap-2 mt-4"><span className={`px-3 py-1 rounded-full text-xs font-semibold border ${isVendor ? 'bg-slate-800 text-slate-300 border-slate-700' : 'bg-pink-50 text-pink-600 border-pink-100'}`}>{isVendor ? 'Nhiếp ảnh gia' : 'Cặp đôi'}</span></div>
        </div>
        <div className="flex-1 overflow-y-auto p-6 pb-24 space-y-6">
          <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
            {[ 
              {icon: <Settings size={20}/>, label: 'Cài đặt tài khoản', action: () => setCurrentScreen('account_settings')}, 
              {icon: isVendor ? <FileText size={20}/> : <CreditCard size={20}/>, label: isVendor ? 'Quản lý dịch vụ' : 'Phương thức thanh toán', action: () => isVendor ? setCurrentScreen('service_management') : null}, 
              {icon: <Shield size={20}/>, label: 'Bảo mật', action: () => null} 
            ].map((item, idx) => (
              <button key={idx} onClick={item.action} className="w-full flex items-center justify-between p-4 border-b border-slate-50 hover:bg-slate-50 text-slate-700 active:bg-slate-100 transition-colors">
                <div className="flex items-center gap-4"><div className="text-slate-400">{item.icon}</div><span className="font-medium text-sm">{item.label}</span></div>
                <ChevronRight size={18} className="text-slate-300" />
              </button>
            ))}
          </div>
          <button onClick={() => setCurrentScreen('login')} className="w-full bg-red-50 text-red-600 py-4 rounded-2xl font-bold text-sm flex items-center justify-center gap-2 mt-6 hover:bg-red-100 transition-colors"><LogOut size={18} /> Đăng xuất</button>
        </div>
      </div>
    );
  };

  const ScreenSearch = () => (
    <div className="h-full flex flex-col bg-slate-50 animate-in fade-in duration-300">
      <div className="bg-white px-6 pt-12 pb-4 shadow-sm sticky top-0 z-10">
        <h2 className="text-2xl font-bold text-slate-800 mb-4">Khám phá</h2>
        <div className="relative">
          <Search className="absolute left-4 top-3.5 text-slate-400" size={20} />
          <input type="text" placeholder="Tìm kiếm dịch vụ, nhà hàng..." className="w-full pl-12 pr-4 py-3 bg-slate-100 border-none rounded-2xl focus:ring-2 focus:ring-pink-500 outline-none transition-all text-slate-700" />
        </div>
      </div>
      <div className="flex-1 overflow-y-auto p-6 pb-24 space-y-6">
        <div className="grid grid-cols-4 gap-2">
            {[ 
              {icon: <Home size={24}/>, label: 'Nhà hàng', bg: 'bg-orange-50', color: 'text-orange-500'}, 
              {icon: <Camera size={24}/>, label: 'Chụp ảnh', bg: 'bg-blue-50', color: 'text-blue-500'} 
            ].map((cat, idx) => (
              <div key={idx} className="flex flex-col items-center gap-2 cursor-pointer group">
                <div className={`w-14 h-14 rounded-2xl flex items-center justify-center ${cat.bg} ${cat.color}`}>{cat.icon}</div>
                <span className="text-xs font-medium text-slate-600 text-center">{cat.label}</span>
              </div>
            ))}
        </div>
      </div>
    </div>
  );

  const ScreenGuestList = () => {
    const [activeTab, setActiveTab] = useState('all');
    const guests = [
      { id: 1, name: 'Phạm Văn A', group: 'Gia đình nhà Gái', status: 'attending' },
      { id: 2, name: 'Nguyễn Thị B', group: 'Bạn đại học', status: 'pending' },
      { id: 3, name: 'Lê C', group: 'Đồng nghiệp', status: 'declined' },
      { id: 4, name: 'Trần D', group: 'Gia đình nhà Trai', status: 'attending' },
      { id: 5, name: 'Đỗ E', group: 'Bạn cấp 3', status: 'attending' },
    ];

    const filtered = activeTab === 'all' ? guests : guests.filter(g => g.status === activeTab);

    return (
      <div className="flex flex-col h-full bg-slate-50 relative pb-20 animate-in slide-in-from-right-4 duration-300">
        <div className="bg-white px-6 pt-12 pb-4 shadow-sm flex flex-col sticky top-0 z-10">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center">
              <button onClick={() => setCurrentScreen('home')} className="mr-4 text-slate-500 hover:text-slate-800 transition-colors"><ArrowLeft size={24}/></button>
              <h2 className="text-xl font-bold text-slate-800">Khách mời</h2>
            </div>
            <button className="text-pink-600 bg-pink-50 p-2 rounded-xl hover:bg-pink-100 transition-colors"><UserPlus size={20}/></button>
          </div>
          <div className="relative">
            <Search className="absolute left-3 top-2.5 text-slate-400" size={18} />
            <input type="text" placeholder="Tìm tên khách..." className="w-full pl-10 pr-4 py-2.5 bg-slate-100 border-none rounded-xl text-sm outline-none focus:ring-2 focus:ring-pink-500" />
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          <div className="grid grid-cols-3 gap-3">
            <div className="bg-white p-3 rounded-2xl shadow-sm border border-slate-100 text-center"><p className="text-xs text-slate-500 mb-1">Tổng cộng</p><p className="text-xl font-bold text-slate-800">120</p></div>
            <div className="bg-white p-3 rounded-2xl shadow-sm border border-slate-100 text-center"><p className="text-xs text-slate-500 mb-1">Tham gia</p><p className="text-xl font-bold text-emerald-500">85</p></div>
            <div className="bg-white p-3 rounded-2xl shadow-sm border border-slate-100 text-center"><p className="text-xs text-slate-500 mb-1">Chờ</p><p className="text-xl font-bold text-orange-500">25</p></div>
          </div>

          <div className="flex space-x-2 overflow-x-auto pb-1 hide-scrollbar">
            {[
              { id: 'all', label: 'Tất cả' }, { id: 'attending', label: 'Tham gia' }, { id: 'pending', label: 'Chờ XN' }, { id: 'declined', label: 'Từ chối' },
            ].map(tab => (
              <button key={tab.id} onClick={() => setActiveTab(tab.id)} className={`px-4 py-2 rounded-full text-sm font-semibold whitespace-nowrap transition-colors ${activeTab === tab.id ? 'bg-slate-800 text-white' : 'bg-white text-slate-500 border border-slate-200'}`}>
                {tab.label}
              </button>
            ))}
          </div>

          <div className="space-y-3">
            {filtered.map(guest => (
              <div key={guest.id} className="bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex items-center justify-between">
                <div><h4 className="font-semibold text-slate-800">{guest.name}</h4><p className="text-xs text-slate-500">{guest.group}</p></div>
                {guest.status === 'attending' && <span className="px-3 py-1 bg-emerald-50 text-emerald-600 text-[10px] font-bold uppercase rounded-lg">Tham gia</span>}
                {guest.status === 'pending' && <span className="px-3 py-1 bg-orange-50 text-orange-600 text-[10px] font-bold uppercase rounded-lg">Chờ XN</span>}
                {guest.status === 'declined' && <span className="px-3 py-1 bg-red-50 text-red-600 text-[10px] font-bold uppercase rounded-lg">Từ chối</span>}
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  };

  const ScreenSharePlan = () => (
    <div className="flex flex-col h-full bg-slate-50 relative pb-20 animate-in slide-in-from-bottom-4 duration-300">
      <div className="bg-white px-6 pt-12 pb-4 shadow-sm flex items-center sticky top-0 z-10"><button onClick={() => setCurrentScreen('home')} className="mr-4 text-slate-500 hover:text-slate-800 transition-colors"><ArrowLeft size={24}/></button><h2 className="text-xl font-bold text-slate-800">Đồng bộ Kế hoạch</h2></div>
      <div className="flex-1 overflow-y-auto p-6 flex flex-col items-center">
        <div className="flex items-center justify-center gap-4 my-8">
          <div className="w-20 h-20 bg-pink-100 rounded-full flex items-center justify-center border-4 border-white shadow-lg shadow-pink-100 z-10"><User size={32} className="text-pink-500" /></div>
          <div className="w-12 h-1 border-t-2 border-dashed border-pink-300 relative"><Heart size={16} className="text-pink-500 absolute -top-2 left-1/2 -translate-x-1/2 bg-slate-50 px-1" /></div>
          <div className="w-20 h-20 bg-slate-100 rounded-full flex items-center justify-center border-4 border-white shadow-lg shadow-slate-200 z-10"><UserPlus size={32} className="text-slate-400" /></div>
        </div>
        <div className="text-center mb-8"><h3 className="text-xl font-bold text-slate-800">Mời nửa kia của bạn</h3><p className="text-slate-500 text-sm mt-2 max-w-[80%] mx-auto">Đồng bộ tài khoản để cả hai có thể cùng xem và chỉnh sửa chung một kế hoạch cưới.</p></div>
        <div className="w-full bg-white p-6 rounded-3xl shadow-sm border border-slate-100 mb-6">
          <p className="text-center text-sm text-slate-500 mb-3 font-medium">Mã kết nối của bạn</p>
          <div className="bg-slate-50 py-4 rounded-2xl text-center border border-slate-200 mb-4"><span className="text-3xl font-bold text-slate-800 tracking-[0.2em]">8A9B2C</span></div>
          <div className="flex gap-3">
            <button className="flex-1 bg-pink-600 text-white py-3.5 rounded-xl font-bold text-sm shadow-sm hover:bg-pink-700 transition-colors flex items-center justify-center gap-2"><Share2 size={18} /> Chia sẻ mã</button>
            <button className="flex-1 bg-slate-100 text-slate-700 py-3.5 rounded-xl font-bold text-sm hover:bg-slate-200 transition-colors flex items-center justify-center gap-2"><LinkIcon size={18} /> Sao chép link</button>
          </div>
        </div>
        <div className="w-full bg-white p-4 rounded-2xl shadow-sm border border-slate-100 flex items-center justify-between opacity-50 grayscale">
          <div className="flex items-center gap-3"><div className="w-10 h-10 bg-slate-200 rounded-full"></div><div><p className="font-semibold text-slate-800 text-sm">Chưa có ai kết nối</p><p className="text-xs text-slate-500">Chờ xác nhận...</p></div></div>
        </div>
      </div>
    </div>
  );

  const ScreenServiceManagement = () => (
    <div className="h-full flex flex-col bg-slate-50 animate-in slide-in-from-right-4 pb-20">
      <div className="bg-white px-6 pt-12 pb-4 shadow-sm border-b border-slate-100 flex items-center sticky top-0 z-10">
        <button onClick={() => setCurrentScreen('profile')} className="mr-4 text-slate-500 hover:text-slate-800"><ArrowLeft size={24}/></button>
        <h2 className="text-xl font-bold text-slate-800">Quản lý dịch vụ</h2>
      </div>
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {[
          { id: 1, name: 'Gói Chụp Pre-Wedding Ngoại Cảnh', price: '15.000.000 ₫', status: 'Đang hoạt động', bookings: 24, img: 'https://images.unsplash.com/photo-1511285560929-80b456fea0bc?w=200' },
          { id: 2, name: 'Chụp Phóng Sự Cưới', price: '8.000.000 ₫', status: 'Đang hoạt động', bookings: 42, img: 'https://images.unsplash.com/photo-1519225421980-715cb0215aed?w=200' },
          { id: 3, name: 'Quay Phim Highlight', price: '10.000.000 ₫', status: 'Tạm ẩn', bookings: 5, img: 'https://images.unsplash.com/photo-1606800052052-a08af7148866?w=200' }
        ].map((svc) => (
          <div key={svc.id} className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden group">
            <div className="flex gap-4 p-4">
              <img src={svc.img} alt={svc.name} className="w-20 h-20 rounded-xl object-cover" />
              <div className="flex-1 flex flex-col justify-between">
                <div className="flex justify-between items-start">
                  <h4 className="font-bold text-slate-800 text-sm line-clamp-2 pr-2">{svc.name}</h4>
                  <button className="text-slate-400 hover:text-pink-600 transition-colors"><Edit size={16}/></button>
                </div>
                <div>
                  <p className="text-pink-600 font-bold">{svc.price}</p>
                  <div className="flex justify-between items-center mt-2">
                    <span className={`text-[10px] font-bold px-2 py-1 rounded-md uppercase ${svc.status === 'Đang hoạt động' ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-500'}`}>{svc.status}</span>
                    <span className="text-[10px] text-slate-500 font-medium">Đã bán: {svc.bookings}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        ))}
        <button className="w-full py-4 bg-pink-50 text-pink-600 border-2 border-dashed border-pink-200 rounded-2xl font-bold flex items-center justify-center gap-2 hover:bg-pink-100 transition-colors mt-4">
          <Plus size={20} /> Thêm dịch vụ mới
        </button>
      </div>
    </div>
  );

  const ScreenAccountSettings = () => (
    <div className="h-full flex flex-col bg-slate-50 animate-in slide-in-from-right-4 pb-20">
      <div className="bg-white px-6 pt-12 pb-4 shadow-sm border-b border-slate-100 flex items-center sticky top-0 z-10">
        <button onClick={() => setCurrentScreen('profile')} className="mr-4 text-slate-500 hover:text-slate-800"><ArrowLeft size={24}/></button>
        <h2 className="text-xl font-bold text-slate-800">Cài đặt tài khoản</h2>
      </div>
      <div className="flex-1 overflow-y-auto p-6 space-y-6">
        <div className="flex flex-col items-center justify-center">
          <div className="relative">
            <div className="w-24 h-24 bg-gradient-to-tr from-slate-200 to-slate-100 rounded-full flex items-center justify-center border-4 border-white shadow-lg overflow-hidden">
               {userData.role === 'vendor' ? <Briefcase size={32} className="text-slate-400" /> : <User size={32} className="text-slate-400" />}
            </div>
            <button className="absolute bottom-0 right-0 p-2 bg-pink-600 text-white rounded-full border-2 border-white hover:bg-pink-700 shadow-sm active:scale-95 transition-all"><Camera size={14}/></button>
          </div>
        </div>
        
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">Tên hiển thị</label>
            <div className="relative">
              <User className="absolute left-3 top-3 text-slate-400" size={20} />
              <input type="text" defaultValue={userData.name} className="w-full pl-10 pr-4 py-3 bg-white border border-slate-200 rounded-xl focus:ring-2 focus:ring-pink-500 outline-none text-slate-800 font-medium" />
            </div>
          </div>
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">Email liên hệ</label>
            <div className="relative">
              <Mail className="absolute left-3 top-3 text-slate-400" size={20} />
              <input type="email" defaultValue={userData.email} className="w-full pl-10 pr-4 py-3 bg-white border border-slate-200 rounded-xl focus:ring-2 focus:ring-pink-500 outline-none text-slate-800 font-medium" />
            </div>
          </div>
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">Số điện thoại</label>
            <div className="relative">
              <Phone className="absolute left-3 top-3 text-slate-400" size={20} />
              <input type="tel" defaultValue="0987654321" className="w-full pl-10 pr-4 py-3 bg-white border border-slate-200 rounded-xl focus:ring-2 focus:ring-pink-500 outline-none text-slate-800 font-medium" />
            </div>
          </div>
        </div>
        
        <button onClick={() => setCurrentScreen('profile')} className="w-full py-4 bg-slate-900 text-white rounded-xl font-bold flex items-center justify-center gap-2 hover:bg-slate-800 active:scale-95 transition-all shadow-lg mt-8">
          <Save size={20} /> Lưu thay đổi
        </button>
      </div>
    </div>
  );

  const BottomNavigation = () => {
    const activeNav = userData.role === 'vendor' ? 
      [ { id: 'home', icon: <Home size={24}/>, label: 'Tổng quan' }, { id: 'vendor_projects', icon: <Briefcase size={24}/>, label: 'Dự án' }, { id: 'vendor_messages', icon: <MessageSquare size={24}/>, label: 'Tin nhắn' }, { id: 'profile', icon: <User size={24}/>, label: 'Cá nhân' } ] : 
      [ { id: 'home', icon: <Home size={24}/>, label: 'Trang chủ' }, { id: 'search', icon: <Search size={24}/>, label: 'Khám phá' }, { id: userData.hasPlan ? 'plan_management' : 'create_plan', icon: <List size={24}/>, label: 'Kế hoạch', matchId: 'plan_management' }, { id: 'profile', icon: <User size={24}/>, label: 'Cá nhân' } ];

    return (
      <div className="absolute bottom-0 w-full bg-white border-t border-slate-100 flex justify-around items-center px-2 py-3 pb-safe z-50 shadow-[0_-4px_20px_rgba(0,0,0,0.03)] sm:rounded-b-[2rem]">
        {activeNav.map((item) => {
          const isActive = currentScreen === item.id || currentScreen === item.matchId;
          return (
            <button key={item.label} onClick={() => setCurrentScreen(item.id)} className={`flex flex-col items-center gap-1 w-16 transition-colors ${isActive ? 'text-pink-600' : 'text-slate-400 hover:text-slate-600'}`}>
              <div className={`p-1.5 rounded-xl ${isActive ? 'bg-pink-50' : 'bg-transparent'} transition-colors`}>{item.icon}</div>
              <span className={`text-[10px] font-semibold ${isActive ? 'text-pink-600' : 'text-slate-500'}`}>{item.label}</span>
            </button>
          )
        })}
      </div>
    );
  };

  return (
    <div className="w-full h-screen bg-slate-900 overflow-hidden flex items-center justify-center font-sans">
      <div className="w-full max-w-[400px] h-full sm:h-[850px] sm:max-h-screen bg-white sm:rounded-[2.5rem] sm:border-[8px] sm:border-slate-800 shadow-2xl relative flex flex-col">
        <div className="flex-1 overflow-hidden sm:rounded-[2rem]">
          {currentScreen === 'splash' && <ScreenSplash />}
          {currentScreen === 'login' && <ScreenLogin />}
          {currentScreen === 'otp' && <ScreenOTP />}
          {currentScreen === 'role_select' && <ScreenRoleSelect />}
          {currentScreen === 'home' && <ScreenHome />}
          {currentScreen === 'search' && <ScreenSearch />}
          {currentScreen === 'profile' && <ScreenProfile />}
          {currentScreen === 'service_detail' && <ScreenServiceDetail />}
          {currentScreen === 'create_plan' && <ScreenCreatePlan />}
          {currentScreen === 'ai_chat' && <ScreenAIChat />}
          {currentScreen === 'plan_management' && <ScreenPlanManagement />}
          {currentScreen === 'notifications' && <ScreenNotifications />}
          {currentScreen === 'guest_list' && <ScreenGuestList />}
          {currentScreen === 'share_plan' && <ScreenSharePlan />}
          
          {/* Vendor Specific */}
          {currentScreen === 'vendor_projects' && <ScreenVendorProjects />}
          {currentScreen === 'vendor_messages' && <ScreenVendorMessages />}
          
          {/* Settings Specific */}
          {currentScreen === 'service_management' && <ScreenServiceManagement />}
          {currentScreen === 'account_settings' && <ScreenAccountSettings />}
        </div>
        {showBottomNav && <BottomNavigation />}
      </div>
    </div>
  );
}