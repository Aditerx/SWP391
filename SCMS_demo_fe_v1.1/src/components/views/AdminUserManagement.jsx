import React, { useState } from 'react';
import { useSCMS } from '../../context/SCMSContext';
import { StatusBadge } from '../common/Badge';
import {
  Users, UserPlus, Search, Filter, Shield, Lock, Unlock, Edit2, Mail, Phone, Calendar,
  CheckCircle2, XCircle, AlertCircle, X, ChevronDown, KeyRound
} from 'lucide-react';

export function AdminUserManagement() {
  const { users, roles, addUser, updateUser, updateUserStatus, updateUserRole, language, t } = useSCMS();
  const [searchTerm, setSearchTerm] = useState('');
  const [roleFilter, setRoleFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState('all');

  // Modal states
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [roleChangingUser, setRoleChangingUser] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Form states
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phone: '',
    roleId: 5,
    roleName: 'Member',
    password: '',
    status: 'Active'
  });
  const [formErrors, setFormErrors] = useState({});

  const filteredUsers = users.filter(u => {
    const matchesSearch = (u.fullName || u.name || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
      (u.email || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
      (u.phone || '').includes(searchTerm) ||
      (u.code || '').toLowerCase().includes(searchTerm.toLowerCase());

    const matchesRole = roleFilter === 'all' || (u.role || '').toLowerCase() === roleFilter.toLowerCase() || (u.roleName || '').toLowerCase() === roleFilter.toLowerCase();
    const matchesStatus = statusFilter === 'all' || (u.status || '').toLowerCase() === statusFilter.toLowerCase();

    return matchesSearch && matchesRole && matchesStatus;
  });

  const getRoleBadgeColor = (roleName) => {
    const r = (roleName || '').toLowerCase();
    if (r === 'admin') return 'bg-purple-100 text-purple-800 border-purple-300';
    if (r === 'centermanager' || r === 'manager') return 'bg-blue-100 text-blue-800 border-blue-300';
    if (r === 'coach') return 'bg-emerald-100 text-emerald-800 border-emerald-300';
    if (r === 'receptionist') return 'bg-amber-100 text-amber-800 border-amber-300';
    return 'bg-slate-100 text-slate-800 border-slate-300';
  };

  const handleOpenCreate = () => {
    setFormData({
      fullName: '',
      email: '',
      phone: '',
      roleId: roles[0]?.id || 1,
      roleName: roles[0]?.name || 'Admin',
      password: '',
      status: 'Active'
    });
    setFormErrors({});
    setIsCreateOpen(true);
  };

  const handleOpenEdit = (user) => {
    setEditingUser(user);
    setFormData({
      fullName: user.fullName || user.name || '',
      email: user.email || '',
      phone: user.phone || '',
      roleId: user.roleId || 5,
      roleName: user.roleName || 'Member',
      password: '',
      status: user.rawStatus || (user.status === 'suspended' ? 'Locked' : 'Active')
    });
    setFormErrors({});
  };

  const validateForm = () => {
    const errors = {};
    if (!formData.fullName.trim()) errors.fullName = language === 'vi' ? 'Họ và tên không được để trống' : 'Full name is required';
    if (!formData.email.trim()) {
      errors.email = language === 'vi' ? 'Email không được để trống' : 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
      errors.email = language === 'vi' ? 'Email không hợp lệ' : 'Invalid email format';
    }
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    setIsSubmitting(true);
    try {
      await addUser({
        fullName: formData.fullName.trim(),
        email: formData.email.trim(),
        phone: formData.phone.trim(),
        roleId: Number(formData.roleId),
        roleName: roles.find(r => r.id === Number(formData.roleId))?.name,
        password: formData.password || '12345678',
        status: formData.status
      });
      setIsCreateOpen(false);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm() || !editingUser) return;

    setIsSubmitting(true);
    try {
      await updateUser(editingUser.id, {
        fullName: formData.fullName.trim(),
        email: formData.email.trim(),
        phone: formData.phone.trim(),
        roleId: Number(formData.roleId),
        roleName: roles.find(r => r.id === Number(formData.roleId))?.name,
        password: formData.password ? formData.password : undefined,
        status: formData.status
      });
      setEditingUser(null);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleToggleLock = async (user) => {
    const isLocked = user.status === 'suspended' || user.rawStatus === 'Locked';
    const nextStatus = isLocked ? 'Active' : 'Locked';
    const confirmMsg = isLocked
      ? (language === 'vi' ? `Mở khóa tài khoản ${user.email}?` : `Unlock account ${user.email}?`)
      : (language === 'vi' ? `Khóa tài khoản ${user.email}? Người dùng sẽ không thể đăng nhập.` : `Lock account ${user.email}?`);

    if (window.confirm(confirmMsg)) {
      await updateUserStatus(user.id, nextStatus);
    }
  };

  const handleChangeRoleSubmit = async (e) => {
    e.preventDefault();
    if (!roleChangingUser) return;

    setIsSubmitting(true);
    try {
      await updateUserRole(roleChangingUser.id, Number(formData.roleId));
      setRoleChangingUser(null);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white border border-slate-200 rounded-lg p-5 shadow-xs flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 bg-purple-50 text-purple-800 rounded border border-purple-200">
            <Shield className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-900">{t('userManagement')}</h2>
            <p className="text-xs text-slate-500">
              {language === 'vi'
                ? 'Quản trị danh sách tài khoản, vai trò bảo mật & trạng thái truy cập hệ thống SCMS'
                : 'Administer user accounts, security roles and system access states'}
            </p>
          </div>
        </div>

        <button
          onClick={handleOpenCreate}
          className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded text-xs font-bold shadow-xs flex items-center gap-2 cursor-pointer transition-colors"
        >
          <UserPlus className="w-4 h-4 text-purple-400" />
          <span>{language === 'vi' ? 'Thêm Người Dùng' : 'Add New User'}</span>
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white border border-slate-200 rounded-lg p-4 shadow-xs flex flex-col md:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder={language === 'vi' ? 'Tìm theo tên, email, số điện thoại, mã người dùng...' : 'Search by name, email, phone, code...'}
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
            className="w-full bg-slate-50 border border-slate-300 rounded pl-9 pr-3 py-2 text-xs text-slate-900 placeholder-slate-400 focus:outline-none focus:border-purple-600 focus:bg-white"
          />
        </div>

        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-slate-400" />
          <select
            value={roleFilter}
            onChange={e => setRoleFilter(e.target.value)}
            className="bg-slate-50 border border-slate-300 rounded px-3 py-2 text-xs text-slate-900 font-semibold focus:outline-none focus:border-purple-600"
          >
            <option value="all">{language === 'vi' ? 'Tất cả vai trò' : 'All Roles'}</option>
            <option value="admin">Admin (Quản trị viên)</option>
            <option value="manager">CenterManager (Quản lý)</option>
            <option value="coach">Coach (Huấn luyện viên)</option>
            <option value="receptionist">Receptionist (Lễ tân)</option>
            <option value="member">Member (Học viên)</option>
          </select>

          <select
            value={statusFilter}
            onChange={e => setStatusFilter(e.target.value)}
            className="bg-slate-50 border border-slate-300 rounded px-3 py-2 text-xs text-slate-900 font-semibold focus:outline-none focus:border-purple-600"
          >
            <option value="all">{language === 'vi' ? 'Tất cả trạng thái' : 'All Statuses'}</option>
            <option value="active">{language === 'vi' ? 'Hoạt động (Active)' : 'Active'}</option>
            <option value="suspended">{language === 'vi' ? 'Đã khóa (Locked)' : 'Locked'}</option>
            <option value="inactive">{language === 'vi' ? 'Tạm ngừng (Inactive)' : 'Inactive'}</option>
          </select>
        </div>
      </div>

      {/* Users Table */}
      <div className="bg-white border border-slate-200 rounded-lg overflow-hidden shadow-xs">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-700 font-bold uppercase tracking-wider text-[11px]">
              <tr>
                <th className="py-3 px-4">{language === 'vi' ? 'Mã / Họ Tên' : 'Code / User'}</th>
                <th className="py-3 px-4">Email / Phone</th>
                <th className="py-3 px-4">{language === 'vi' ? 'Vai trò (Role)' : 'Role'}</th>
                <th className="py-3 px-4">{language === 'vi' ? 'Trạng thái' : 'Status'}</th>
                <th className="py-3 px-4 text-right">{language === 'vi' ? 'Thao tác' : 'Actions'}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan="5" className="py-8 text-center text-slate-500 text-xs">
                    {t('noData')}
                  </td>
                </tr>
              ) : (
                filteredUsers.map(user => {
                  const isLocked = user.status === 'suspended' || user.rawStatus === 'Locked';
                  return (
                    <tr key={user.id} className="hover:bg-slate-50/80 transition-colors">
                      <td className="py-3.5 px-4">
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full bg-slate-900 text-white font-bold text-xs flex items-center justify-center shrink-0">
                            {(user.fullName || user.name || 'U').charAt(0).toUpperCase()}
                          </div>
                          <div>
                            <div className="font-bold text-slate-900">{user.fullName || user.name}</div>
                            <div className="text-[10px] text-slate-500 font-mono">{user.code}</div>
                          </div>
                        </div>
                      </td>

                      <td className="py-3.5 px-4 text-slate-600">
                        <div className="flex items-center gap-1.5 font-mono">
                          <Mail className="w-3 h-3 text-slate-400" />
                          <span>{user.email}</span>
                        </div>
                        {user.phone && (
                          <div className="flex items-center gap-1.5 font-mono text-[11px] text-slate-500 mt-0.5">
                            <Phone className="w-3 h-3 text-slate-400" />
                            <span>{user.phone}</span>
                          </div>
                        )}
                      </td>

                      <td className="py-3.5 px-4">
                        <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold border ${getRoleBadgeColor(user.roleName || user.role)}`}>
                          <KeyRound className="w-3 h-3" />
                          <span>{user.roleName || user.role}</span>
                        </span>
                      </td>

                      <td className="py-3.5 px-4">
                        {isLocked ? (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-bold bg-rose-50 text-rose-700 border border-rose-200">
                            <Lock className="w-3 h-3" />
                            <span>{language === 'vi' ? 'Đã khóa' : 'Locked'}</span>
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <CheckCircle2 className="w-3 h-3" />
                            <span>{language === 'vi' ? 'Hoạt động' : 'Active'}</span>
                          </span>
                        )}
                      </td>

                      <td className="py-3.5 px-4 text-right">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            onClick={() => handleOpenEdit(user)}
                            className="p-1.5 text-slate-600 hover:text-purple-700 hover:bg-purple-50 rounded border border-transparent hover:border-purple-200 cursor-pointer transition-colors"
                            title={language === 'vi' ? 'Chỉnh sửa thông tin' : 'Edit User'}
                          >
                            <Edit2 className="w-3.5 h-3.5" />
                          </button>

                          <button
                            onClick={() => {
                              setRoleChangingUser(user);
                              setFormData(prev => ({ ...prev, roleId: user.roleId || 5 }));
                            }}
                            className="p-1.5 text-slate-600 hover:text-blue-700 hover:bg-blue-50 rounded border border-transparent hover:border-blue-200 cursor-pointer transition-colors"
                            title={language === 'vi' ? 'Phân lại vai trò' : 'Change Role'}
                          >
                            <KeyRound className="w-3.5 h-3.5" />
                          </button>

                          <button
                            onClick={() => handleToggleLock(user)}
                            className={`p-1.5 rounded border border-transparent cursor-pointer transition-colors ${
                              isLocked
                                ? 'text-emerald-600 hover:text-emerald-800 hover:bg-emerald-50 hover:border-emerald-200'
                                : 'text-rose-600 hover:text-rose-800 hover:bg-rose-50 hover:border-rose-200'
                            }`}
                            title={isLocked ? t('unlockUser') : t('lockUser')}
                          >
                            {isLocked ? <Unlock className="w-3.5 h-3.5" /> : <Lock className="w-3.5 h-3.5" />}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Create / Edit User Modal */}
      {(isCreateOpen || editingUser) && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
          <div role="dialog" className="bg-white border border-slate-200 rounded-lg w-full max-w-lg shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-200 bg-slate-50">
              <div className="flex items-center gap-2.5">
                <div className="p-2 bg-purple-900 text-white rounded">
                  <UserPlus className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-slate-900">
                    {editingUser
                      ? (language === 'vi' ? 'Chỉnh sửa tài khoản người dùng' : 'Edit User Account')
                      : (language === 'vi' ? 'Tạo tài khoản người dùng mới' : 'Create New User Account')}
                  </h3>
                  <p className="text-[11px] text-slate-500">
                    {language === 'vi' ? 'Thông tin đăng nhập & vai trò hệ thống' : 'Credentials & system role'}
                  </p>
                </div>
              </div>

              <button
                type="button"
                onClick={() => {
                  setIsCreateOpen(false);
                  setEditingUser(null);
                }}
                className="p-1 text-slate-400 hover:text-slate-700"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={editingUser ? handleEditSubmit : handleCreateSubmit} className="p-6 space-y-4">
              <div>
                <label className="text-xs font-bold text-slate-700 block mb-1">
                  {language === 'vi' ? 'Họ và tên *' : 'Full Name *'}
                </label>
                <input
                  type="text"
                  required
                  value={formData.fullName}
                  onChange={e => setFormData({ ...formData, fullName: e.target.value })}
                  placeholder="Nguyễn Văn A"
                  className="w-full h-9.5 px-3 bg-slate-50 border border-slate-300 rounded text-xs text-slate-900 focus:outline-none focus:border-purple-600 focus:bg-white"
                />
                {formErrors.fullName && <p className="text-[11px] text-rose-600 mt-1">{formErrors.fullName}</p>}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-bold text-slate-700 block mb-1">
                    Email <span className="text-rose-600">*</span>
                  </label>
                  <input
                    type="email"
                    required
                    value={formData.email}
                    onChange={e => setFormData({ ...formData, email: e.target.value })}
                    placeholder="user@scms.com"
                    className="w-full h-9.5 px-3 bg-slate-50 border border-slate-300 rounded text-xs text-slate-900 focus:outline-none focus:border-purple-600 focus:bg-white"
                  />
                  {formErrors.email && <p className="text-[11px] text-rose-600 mt-1">{formErrors.email}</p>}
                </div>

                <div>
                  <label className="text-xs font-bold text-slate-700 block mb-1">
                    {language === 'vi' ? 'Số điện thoại' : 'Phone Number'}
                  </label>
                  <input
                    type="tel"
                    value={formData.phone}
                    onChange={e => setFormData({ ...formData, phone: e.target.value })}
                    placeholder="0901234567"
                    className="w-full h-9.5 px-3 bg-slate-50 border border-slate-300 rounded text-xs text-slate-900 focus:outline-none focus:border-purple-600 focus:bg-white"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-bold text-slate-700 block mb-1">
                    {language === 'vi' ? 'Vai trò (Role) *' : 'System Role *'}
                  </label>
                  <select
                    value={formData.roleId}
                    onChange={e => setFormData({ ...formData, roleId: Number(e.target.value) })}
                    className="w-full h-9.5 px-3 bg-slate-50 border border-slate-300 rounded text-xs text-slate-900 font-semibold focus:outline-none focus:border-purple-600"
                  >
                    {roles.map(r => (
                      <option key={r.id} value={r.id}>
                        {r.name} ({r.description?.slice(0, 30)}...)
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="text-xs font-bold text-slate-700 block mb-1">
                    {language === 'vi' ? 'Trạng thái tài khoản' : 'Account Status'}
                  </label>
                  <select
                    value={formData.status}
                    onChange={e => setFormData({ ...formData, status: e.target.value })}
                    className="w-full h-9.5 px-3 bg-slate-50 border border-slate-300 rounded text-xs text-slate-900 font-semibold focus:outline-none focus:border-purple-600"
                  >
                    <option value="Active">{language === 'vi' ? 'Hoạt động (Active)' : 'Active'}</option>
                    <option value="Locked">{language === 'vi' ? 'Đã khóa (Locked)' : 'Locked'}</option>
                    <option value="Inactive">{language === 'vi' ? 'Tạm ngừng (Inactive)' : 'Inactive'}</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-700 block mb-1">
                  {editingUser
                    ? (language === 'vi' ? 'Mật khẩu mới (Bỏ trống nếu giữ nguyên)' : 'New Password (leave empty to keep current)')
                    : (language === 'vi' ? 'Mật khẩu khởi tạo (Mặc định: 12345678)' : 'Initial Password (default: 12345678)')}
                </label>
                <input
                  type="password"
                  value={formData.password}
                  onChange={e => setFormData({ ...formData, password: e.target.value })}
                  placeholder="••••••••"
                  className="w-full h-9.5 px-3 bg-slate-50 border border-slate-300 rounded text-xs text-slate-900 focus:outline-none focus:border-purple-600 focus:bg-white"
                />
              </div>

              <div className="pt-4 border-t border-slate-200 flex items-center justify-end gap-2.5">
                <button
                  type="button"
                  onClick={() => {
                    setIsCreateOpen(false);
                    setEditingUser(null);
                  }}
                  className="px-4 py-2 border border-slate-300 hover:bg-slate-100 rounded text-xs font-bold text-slate-700"
                >
                  {t('cancel')}
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-4 py-2 bg-slate-900 hover:bg-slate-800 disabled:opacity-50 text-white rounded text-xs font-bold shadow-xs cursor-pointer"
                >
                  {isSubmitting ? (language === 'vi' ? 'Đang lưu...' : 'Saving...') : t('saveChanges')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Change Role Quick Modal */}
      {roleChangingUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
          <div role="dialog" className="bg-white border border-slate-200 rounded-lg w-full max-w-sm shadow-2xl p-5 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <KeyRound className="w-4 h-4 text-purple-600" />
                <span>{language === 'vi' ? 'Đổi vai trò người dùng' : 'Change User Role'}</span>
              </h3>
              <button onClick={() => setRoleChangingUser(null)} className="p-1 text-slate-400 hover:text-slate-700">
                <X className="w-4 h-4" />
              </button>
            </div>

            <p className="text-xs text-slate-600">
              {language === 'vi' ? 'Chọn vai trò mới cho tài khoản' : 'Select a new role for'}{' '}
              <strong className="text-slate-900">{roleChangingUser.email}</strong>:
            </p>

            <form onSubmit={handleChangeRoleSubmit} className="space-y-4">
              <select
                value={formData.roleId}
                onChange={e => setFormData({ ...formData, roleId: Number(e.target.value) })}
                className="w-full h-10 px-3 bg-slate-50 border border-slate-300 rounded text-xs text-slate-900 font-bold focus:outline-none focus:border-purple-600"
              >
                {roles.map(r => (
                  <option key={r.id} value={r.id}>
                    {r.name} - {r.description}
                  </option>
                ))}
              </select>

              <div className="flex items-center justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setRoleChangingUser(null)}
                  className="px-3.5 py-1.5 border border-slate-300 hover:bg-slate-100 rounded text-xs font-bold text-slate-700"
                >
                  {t('cancel')}
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-3.5 py-1.5 bg-slate-900 hover:bg-slate-800 disabled:opacity-50 text-white rounded text-xs font-bold cursor-pointer"
                >
                  {isSubmitting ? '...' : t('confirm')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
