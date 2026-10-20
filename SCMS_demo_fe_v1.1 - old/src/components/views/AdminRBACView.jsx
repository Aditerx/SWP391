import React, { useState, useEffect } from 'react';
import { useSCMS } from '../../context/SCMSContext';
import {
  Shield, KeyRound, CheckSquare, Square, Save, RotateCcw, Info, CheckCircle2,
  Users, Calendar, Package, BarChart3, History, Award, CreditCard, Activity, HelpCircle
} from 'lucide-react';

export function AdminRBACView() {
  const { roles, permissions, updateRolePermissions, language, t } = useSCMS();
  const [selectedRoleId, setSelectedRoleId] = useState(roles[0]?.id || 1);
  const [selectedPermIds, setSelectedPermIds] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDirty, setIsDirty] = useState(false);

  const selectedRole = roles.find(r => r.id === selectedRoleId) || roles[0];

  useEffect(() => {
    if (selectedRole) {
      const currentIds = (selectedRole.permissions || []).map(p => p.id);
      setSelectedPermIds(currentIds);
      setIsDirty(false);
    }
  }, [selectedRoleId, roles]);

  const handleTogglePerm = (permId) => {
    setSelectedPermIds(prev => {
      const next = prev.includes(permId)
        ? prev.filter(id => id !== permId)
        : [...prev, permId];
      setIsDirty(true);
      return next;
    });
  };

  const handleSelectAll = () => {
    setSelectedPermIds(permissions.map(p => p.id));
    setIsDirty(true);
  };

  const handleDeselectAll = () => {
    setSelectedPermIds([]);
    setIsDirty(true);
  };

  const handleSave = async () => {
    if (!selectedRole) return;
    setIsSubmitting(true);
    try {
      await updateRolePermissions(selectedRole.id, selectedPermIds);
      setIsDirty(false);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReset = () => {
    if (selectedRole) {
      setSelectedPermIds((selectedRole.permissions || []).map(p => p.id));
      setIsDirty(false);
    }
  };

  const getPermissionCategory = (permName) => {
    if (['MANAGE_USERS', 'MANAGE_RBAC', 'VIEW_AUDIT_LOG'].includes(permName)) return 'System Administration';
    if (['MANAGE_CLASSES', 'MANAGE_PACKAGES', 'VIEW_REPORTS'].includes(permName)) return 'Center Operations';
    if (['REGISTER_MEMBER', 'MANAGE_SUBSCRIPTIONS', 'PROCESS_PAYMENT', 'HANDLE_SUPPORT'].includes(permName)) return 'Reception & Memberships';
    return 'Coaching & Training';
  };

  const categorizedPermissions = permissions.reduce((acc, perm) => {
    const cat = getPermissionCategory(perm.name);
    if (!acc[cat]) acc[cat] = [];
    acc[cat].push(perm);
    return acc;
  }, {});

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white border border-slate-200 rounded-lg p-5 shadow-xs flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 bg-purple-50 text-purple-800 rounded border border-purple-200">
            <KeyRound className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-900">{t('rbacManagement')}</h2>
            <p className="text-xs text-slate-500">
              {language === 'vi'
                ? 'Thiết lập ma trận phân quyền dựa trên vai trò (Role-Based Access Control) cho hệ thống SCMS'
                : 'Configure Role-Based Access Control matrix for SCMS system'}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {isDirty && (
            <button
              onClick={handleReset}
              disabled={isSubmitting}
              className="px-3.5 py-1.5 border border-slate-300 hover:bg-slate-50 text-slate-700 rounded text-xs font-bold flex items-center gap-1.5 transition-colors cursor-pointer"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>{language === 'vi' ? 'Hoàn tác' : 'Reset'}</span>
            </button>
          )}

          <button
            onClick={handleSave}
            disabled={isSubmitting || !isDirty}
            className="px-4 py-2 bg-purple-900 hover:bg-purple-800 disabled:opacity-50 text-white rounded text-xs font-bold shadow-xs flex items-center gap-2 transition-colors cursor-pointer"
          >
            <Save className="w-4 h-4" />
            <span>{isSubmitting ? (language === 'vi' ? 'Đang lưu...' : 'Saving...') : t('savePermissions')}</span>
          </button>
        </div>
      </div>

      {/* Info Notice about Separation of Roles */}
      <div className="bg-purple-50/70 border border-purple-200 rounded-lg p-4 flex items-start gap-3 text-xs text-purple-900 leading-relaxed">
        <Info className="w-4 h-4 text-purple-700 shrink-0 mt-0.5" />
        <div>
          <strong className="font-bold">
            {language === 'vi' ? 'Quy tắc phân tách đặc quyền bảo mật:' : 'Security Separation Directive:'}
          </strong>
          <span className="block mt-0.5 text-purple-800">
            {language === 'vi'
              ? 'Vai trò Admin tập trung vào cấu trúc bảo mật (quản trị người dùng & phân quyền RBAC). Center Manager tập trung vào vận hành (lớp học, gói tập, báo cáo). Receptionist quản lý học viên tại quầy và đăng ký/gia hạn gói tập.'
              : 'Admin role focuses on security governance (Users & RBAC). Center Manager handles operational workflows (Classes, Packages, Reports). Receptionist handles counter registration & subscription renewals.'}
          </span>
        </div>
      </div>

      {/* Main Grid: Roles Selector (Left) + Permissions Matrix (Right) */}
      <div className="grid grid-cols-1 lg:grid-cols-[280px_1fr] gap-6">
        {/* Left: Roles List */}
        <div className="bg-white border border-slate-200 rounded-lg p-4 shadow-xs space-y-2 h-fit">
          <h3 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-3 px-1">
            {language === 'vi' ? 'Danh sách vai trò' : 'System Roles'}
          </h3>

          <div className="space-y-1.5">
            {roles.map(r => {
              const isSelected = r.id === selectedRoleId;
              const permCount = (r.permissions || []).length;
              return (
                <button
                  key={r.id}
                  onClick={() => setSelectedRoleId(r.id)}
                  className={`w-full text-left p-3 rounded-lg border transition-all cursor-pointer ${
                    isSelected
                      ? 'bg-purple-50 border-purple-300 text-purple-950 font-bold shadow-2xs'
                      : 'bg-white border-slate-200 hover:bg-slate-50 text-slate-700 font-medium'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="text-xs">{r.name}</span>
                    <span className={`px-2 py-0.5 rounded-full text-[10px] font-mono ${
                      isSelected ? 'bg-purple-200 text-purple-900 font-bold' : 'bg-slate-100 text-slate-600'
                    }`}>
                      {permCount} {language === 'vi' ? 'quyền' : 'perms'}
                    </span>
                  </div>
                  <p className="text-[11px] text-slate-500 font-normal mt-1 line-clamp-2 leading-relaxed">
                    {r.description}
                  </p>
                </button>
              );
            })}
          </div>
        </div>

        {/* Right: Permissions Checklist Matrix */}
        <div className="bg-white border border-slate-200 rounded-lg p-6 shadow-xs space-y-6">
          {selectedRole ? (
            <>
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-200">
                <div>
                  <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                    <Shield className="w-4 h-4 text-purple-700" />
                    <span>{language === 'vi' ? `Quyền hạn cấp cho vai trò: ${selectedRole.name}` : `Permissions for role: ${selectedRole.name}`}</span>
                  </h3>
                  <p className="text-xs text-slate-500 mt-0.5">{selectedRole.description}</p>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={handleSelectAll}
                    className="px-2.5 py-1 text-xs font-bold text-purple-700 hover:bg-purple-50 rounded transition-colors cursor-pointer"
                  >
                    {language === 'vi' ? 'Chọn tất cả' : 'Select all'}
                  </button>
                  <span className="text-slate-300">|</span>
                  <button
                    onClick={handleDeselectAll}
                    className="px-2.5 py-1 text-xs font-bold text-slate-600 hover:bg-slate-100 rounded transition-colors cursor-pointer"
                  >
                    {language === 'vi' ? 'Bỏ chọn tất cả' : 'Deselect all'}
                  </button>
                </div>
              </div>

              {/* Categorized Permissions Grid */}
              <div className="space-y-6">
                {Object.entries(categorizedPermissions).map(([category, perms]) => (
                  <div key={category} className="space-y-3">
                    <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider bg-slate-50 px-3 py-1.5 rounded border border-slate-200">
                      {category}
                    </h4>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                      {perms.map(perm => {
                        const isChecked = selectedPermIds.includes(perm.id);
                        return (
                          <div
                            key={perm.id}
                            onClick={() => handleTogglePerm(perm.id)}
                            className={`p-3 rounded-lg border flex items-start gap-3 cursor-pointer transition-all ${
                              isChecked
                                ? 'bg-purple-50/60 border-purple-300 text-purple-950'
                                : 'bg-white border-slate-200 hover:border-slate-300 text-slate-700 hover:bg-slate-50/50'
                            }`}
                          >
                            <div className="mt-0.5 shrink-0">
                              {isChecked ? (
                                <CheckSquare className="w-4 h-4 text-purple-700" />
                              ) : (
                                <Square className="w-4 h-4 text-slate-300" />
                              )}
                            </div>

                            <div className="min-w-0 flex-1">
                              <div className="text-xs font-bold font-mono tracking-tight text-slate-900">
                                {perm.name}
                              </div>
                              <p className="text-[11px] text-slate-500 mt-0.5 leading-relaxed">
                                {perm.description}
                              </p>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                ))}
              </div>
            </>
          ) : (
            <div className="py-12 text-center text-slate-400 text-xs">
              {language === 'vi' ? 'Vui lòng chọn vai trò để cấu hình phân quyền.' : 'Select a role to configure permissions.'}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
