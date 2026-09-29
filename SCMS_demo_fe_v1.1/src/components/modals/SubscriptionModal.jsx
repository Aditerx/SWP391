import React, { useState, useMemo } from 'react';
import { useSCMS } from '../../context/SCMSContext';
import { formatCurrency, formatDate } from '../../locales/translations';
import {
  Award, Calendar, CreditCard, CheckCircle2, AlertCircle, X, Sparkles, Clock, ArrowRight, ShieldCheck
} from 'lucide-react';

export function SubscriptionModal({ isOpen, onClose, member, isRenewal = false }) {
  const { packages, subscribeMemberPackage, renewMemberSubscription, language, t } = useSCMS();
  const [selectedPackageId, setSelectedPackageId] = useState(
    member?.currentPackageId || (packages.find(p => p.status === 'active')?.id) || packages[0]?.id
  );
  const [customStartDate, setCustomStartDate] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('Cash');
  const [notes, setNotes] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  const activePackages = useMemo(() => packages.filter(p => p.status === 'active' || p.id === selectedPackageId), [packages, selectedPackageId]);

  const selectedPackage = useMemo(() => {
    return packages.find(p => String(p.id) === String(selectedPackageId)) || activePackages[0];
  }, [packages, activePackages, selectedPackageId]);

  // Calculate start date & preview end date
  const calculatedDates = useMemo(() => {
    const today = new Date();
    const todayStr = today.toISOString().split('T')[0];

    let startDateStr = customStartDate || todayStr;

    // If renewal and member has joinDate/expiry date in future
    if (isRenewal && !customStartDate && member?.expiryDate) {
      const expDate = new Date(member.expiryDate);
      if (expDate >= today) {
        expDate.setDate(expDate.getDate() + 1);
        startDateStr = expDate.toISOString().split('T')[0];
      }
    }

    const durationDays = selectedPackage?.durationDays || (selectedPackage?.durationMonths ? selectedPackage.durationMonths * 30 : 30);
    const startDateObj = new Date(startDateStr);
    const endDateObj = new Date(startDateObj);
    endDateObj.setDate(endDateObj.getDate() + durationDays);
    const endDateStr = endDateObj.toISOString().split('T')[0];

    return {
      startDate: startDateStr,
      endDate: endDateStr,
      durationDays
    };
  }, [customStartDate, isRenewal, member, selectedPackage]);

  if (!isOpen || !member) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!selectedPackage) {
      setError(language === 'vi' ? 'Vui lòng chọn gói tập' : 'Please select a package');
      return;
    }

    setError('');
    setIsSubmitting(true);
    try {
      const payload = {
        packageId: selectedPackage.id,
        startDate: calculatedDates.startDate,
        durationDays: calculatedDates.durationDays,
        paymentMethod,
        notes: notes.trim() || undefined,
        isRenewal: Boolean(isRenewal)
      };

      if (isRenewal) {
        await renewMemberSubscription(member.id, payload);
      } else {
        await subscribeMemberPackage(member.id, payload);
      }
      onClose();
    } catch (err) {
      setError(err?.message || (language === 'vi' ? 'Không thể thực hiện đăng ký gói' : 'Failed to process subscription'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
      <div role="dialog" className="bg-white border border-slate-200 rounded-xl w-full max-w-lg shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-150">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-200 bg-slate-50">
          <div className="flex items-center gap-2.5">
            <div className={`p-2 rounded text-white ${isRenewal ? 'bg-amber-600' : 'bg-slate-900'}`}>
              <Award className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-slate-900">
                {isRenewal
                  ? (language === 'vi' ? 'Gia Hạn Gói Tập Tại Quầy' : 'Renew Membership Subscription')
                  : (language === 'vi' ? 'Đăng Ký Gói Tập Mới' : 'Register New Subscription')}
              </h3>
              <p className="text-[11px] text-slate-500">
                {member.name} ({member.code}) &bull; {member.phone}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="p-1 text-slate-400 hover:text-slate-700 cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-md text-rose-700 text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Member Current Status summary */}
          <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-xs space-y-1">
            <div className="flex justify-between">
              <span className="text-slate-500">{language === 'vi' ? 'Gói hiện tại:' : 'Current Package:'}</span>
              <span className="font-bold text-slate-900">{member.currentPackageName || (language === 'vi' ? 'Chưa có gói' : 'None')}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">{language === 'vi' ? 'Trạng thái hội viên:' : 'Status:'}</span>
              <span className="font-semibold text-emerald-700 capitalize">{member.membershipStatus || 'Active'}</span>
            </div>
          </div>

          {/* Package Selector */}
          <div>
            <label className="text-xs font-bold text-slate-700 block mb-1.5">
              {language === 'vi' ? 'Chọn Gói Thành Viên *' : 'Select Membership Package *'}
            </label>
            <select
              value={selectedPackageId}
              onChange={e => setSelectedPackageId(e.target.value)}
              className="w-full h-10 px-3 bg-slate-50 border border-slate-300 rounded text-xs text-slate-900 font-bold focus:outline-none focus:border-blue-600 focus:bg-white"
            >
              {activePackages.map(pkg => (
                <option key={pkg.id} value={pkg.id}>
                  {language === 'vi' ? pkg.nameVi : pkg.name} - {formatCurrency(pkg.price, language)} ({pkg.durationMonths || Math.round((pkg.durationDays || 30)/30)} {language === 'vi' ? 'tháng' : 'months'})
                </option>
              ))}
            </select>
          </div>

          {/* Date Duration Preview */}
          <div className="grid grid-cols-2 gap-3 p-3.5 bg-blue-50/60 border border-blue-200 rounded-lg text-xs">
            <div>
              <span className="text-blue-900 font-bold block mb-1 flex items-center gap-1">
                <Calendar className="w-3.5 h-3.5" />
                <span>{language === 'vi' ? 'Ngày bắt đầu' : 'Start Date'}</span>
              </span>
              <span className="font-mono font-bold text-slate-900 text-sm">
                {calculatedDates.startDate}
              </span>
            </div>

            <div>
              <span className="text-blue-900 font-bold block mb-1 flex items-center gap-1">
                <Clock className="w-3.5 h-3.5" />
                <span>{language === 'vi' ? 'Hạn sử dụng mới' : 'New Expiry Date'}</span>
              </span>
              <span className="font-mono font-bold text-emerald-700 text-sm">
                {calculatedDates.endDate}
              </span>
            </div>
          </div>

          {/* Payment Method */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="text-xs font-bold text-slate-700 block mb-1">
                {language === 'vi' ? 'Hình thức thanh toán *' : 'Payment Method *'}
              </label>
              <select
                value={paymentMethod}
                onChange={e => setPaymentMethod(e.target.value)}
                className="w-full h-9.5 px-3 bg-slate-50 border border-slate-300 rounded text-xs text-slate-900 font-semibold focus:outline-none focus:border-blue-600"
              >
                <option value="Cash">{language === 'vi' ? 'Tiền mặt tại quầy (Cash)' : 'Cash at Counter'}</option>
                <option value="BankTransfer">{language === 'vi' ? 'Chuyển khoản VietQR (Bank Transfer)' : 'Bank Transfer (VietQR)'}</option>
                <option value="CreditCard">{language === 'vi' ? 'Quẹt thẻ máy POS (Card POS)' : 'Card POS Machine'}</option>
                <option value="EWallet">{language === 'vi' ? 'Ví điện tử Momo/ZaloPay' : 'E-Wallet'}</option>
              </select>
            </div>

            <div>
              <label className="text-xs font-bold text-slate-700 block mb-1">
                {language === 'vi' ? 'Số tiền thanh toán' : 'Total Amount'}
              </label>
              <div className="w-full h-9.5 px-3 bg-slate-100 border border-slate-200 rounded text-xs text-slate-900 font-bold flex items-center justify-between font-mono">
                <span>{formatCurrency(selectedPackage?.price || 0, language)}</span>
                <span className="text-[10px] text-slate-500 font-normal">VND</span>
              </div>
            </div>
          </div>

          {/* Notes */}
          <div>
            <label className="text-xs font-bold text-slate-700 block mb-1">
              {language === 'vi' ? 'Ghi chú tiếp nhận tại quầy' : 'Front-desk Notes'}
            </label>
            <input
              type="text"
              value={notes}
              onChange={e => setNotes(e.target.value)}
              placeholder={language === 'vi' ? 'Ví dụ: Học viên thanh toán tiền mặt có hóa đơn VAT...' : 'e.g. Paid in cash at counter with receipt...'}
              className="w-full h-9.5 px-3 bg-slate-50 border border-slate-300 rounded text-xs text-slate-900 focus:outline-none focus:border-blue-600 focus:bg-white"
            />
          </div>

          {/* Actions */}
          <div className="pt-4 border-t border-slate-200 flex items-center justify-end gap-2.5">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 border border-slate-300 hover:bg-slate-100 rounded text-xs font-bold text-slate-700 cursor-pointer"
            >
              {t('cancel')}
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2 bg-slate-900 hover:bg-slate-800 disabled:opacity-50 text-white rounded text-xs font-bold shadow-xs flex items-center gap-2 cursor-pointer"
            >
              {isSubmitting ? (
                <span>{language === 'vi' ? 'Đang xử lý...' : 'Processing...'}</span>
              ) : (
                <>
                  <span>{isRenewal ? (language === 'vi' ? 'Xác Nhận Gia Hạn' : 'Confirm Renewal') : (language === 'vi' ? 'Xác Nhận Đăng Ký' : 'Confirm Subscription')}</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
