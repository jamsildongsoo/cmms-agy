import { useEffect, useState } from 'react';
import axiosInstance from '../api/axios';
import { ShieldCheck, Users, History } from 'lucide-react';

interface SysUser {
  companyId: string;
  id: string;
  name: string;
  roleId: string | null;
  departmentId: string | null;
  position: string | null;
  title: string | null;
  email: string | null;
  phone: string | null;
  useYn: string;
  lastLoginAt: string | null;
  lastLoginIp: string | null;
}
interface Company { id: string; name: string; }
interface LoginHist {
  companyId: string;
  userId: string;
  loginAt: string | null;
  loginIp: string | null;
  loginResult: string;
}

export default function SystemAdmin() {
  const [tab, setTab] = useState<'users' | 'history'>('users');
  const [companies, setCompanies] = useState<Company[]>([]);
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // 사용자 관리
  const [userCompanyId, setUserCompanyId] = useState('');
  const [users, setUsers] = useState<SysUser[]>([]);

  // 로그인 이력
  const [histCompanyId, setHistCompanyId] = useState('');
  const [histUserId, setHistUserId] = useState('');
  const [history, setHistory] = useState<LoginHist[]>([]);

  useEffect(() => {
    axiosInstance.get('/mdm/companies')
      .then(res => setCompanies(res.data))
      .catch(() => setMessage({ type: 'error', text: '회사 목록을 불러오지 못했습니다.' }));
  }, []);

  const fetchUsers = async () => {
    try {
      const res = await axiosInstance.get('/system/users', { params: userCompanyId ? { companyId: userCompanyId } : {} });
      setUsers(res.data);
    } catch {
      setMessage({ type: 'error', text: '사용자 목록을 불러오지 못했습니다.' });
    }
  };

  const fetchHistory = async () => {
    try {
      const params: Record<string, string> = {};
      if (histCompanyId) params.companyId = histCompanyId;
      if (histUserId) params.userId = histUserId;
      const res = await axiosInstance.get('/system/login-history', { params });
      setHistory(res.data);
    } catch {
      setMessage({ type: 'error', text: '로그인 이력을 불러오지 못했습니다.' });
    }
  };

  useEffect(() => { if (tab === 'users') fetchUsers(); }, [tab, userCompanyId]); // eslint-disable-line
  useEffect(() => { if (tab === 'history') fetchHistory(); }, [tab]); // eslint-disable-line

  const toggleUseYn = async (u: SysUser) => {
    const next = u.useYn === 'Y' ? 'N' : 'Y';
    if (!confirm(`[${u.companyId}] ${u.id} 사용여부를 ${next === 'Y' ? '활성(Y)' : '비활성(N)'}으로 변경할까요?`)) return;
    try {
      await axiosInstance.put(`/system/users/${u.companyId}/${u.id}/use-yn`, { useYn: next });
      setMessage({ type: 'success', text: '사용여부를 변경했습니다.' });
      fetchUsers();
    } catch (err: any) {
      setMessage({ type: 'error', text: err.response?.data?.message || '변경 실패' });
    }
  };

  return (
    <div className="space-y-5">
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5">
        <h2 className="text-lg font-bold text-slate-100 flex items-center gap-2">
          <ShieldCheck size={18} className="text-blue-500" /> 시스템 관리 (전 테넌트)
        </h2>
        <p className="text-slate-400 text-xs mt-1">플랫폼 관리자 전용 — 모든 회사의 사용자/로그인 이력 관리</p>
      </div>

      {message && (
        <div className={`text-xs px-3 py-2 rounded-lg border ${message.type === 'success'
          ? 'bg-emerald-500/10 text-emerald-400 border-emerald-800'
          : 'bg-red-500/10 text-red-400 border-red-800'}`}>
          {message.text}
        </div>
      )}

      <div className="flex gap-2">
        <button onClick={() => setTab('users')} className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold ${tab === 'users' ? 'bg-blue-600 text-white' : 'bg-slate-800 text-slate-300'}`}>
          <Users size={14} /> 사용자 관리
        </button>
        <button onClick={() => setTab('history')} className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold ${tab === 'history' ? 'bg-blue-600 text-white' : 'bg-slate-800 text-slate-300'}`}>
          <History size={14} /> 로그인 이력
        </button>
      </div>

      {tab === 'users' && (
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-3">
          <select value={userCompanyId} onChange={e => setUserCompanyId(e.target.value)}
            className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-slate-200">
            <option value="">전체 회사</option>
            {companies.map(c => <option key={c.id} value={c.id}>{c.id} ({c.name})</option>)}
          </select>
          <div className="overflow-x-auto">
            <table className="w-full text-xs text-slate-300">
              <thead className="text-slate-500 border-b border-slate-800">
                <tr>
                  {['회사', '아이디', '이름', '롤', '부서', '직급/직책', '최종로그인', '사용여부'].map(h =>
                    <th key={h} className="text-left px-2 py-2 font-semibold">{h}</th>)}
                </tr>
              </thead>
              <tbody>
                {users.map(u => (
                  <tr key={`${u.companyId}:${u.id}`} className="border-b border-slate-800/60">
                    <td className="px-2 py-2 font-mono">{u.companyId}</td>
                    <td className="px-2 py-2 font-mono">{u.id}</td>
                    <td className="px-2 py-2">{u.name}</td>
                    <td className="px-2 py-2">{u.roleId || '-'}</td>
                    <td className="px-2 py-2">{u.departmentId || '-'}</td>
                    <td className="px-2 py-2">{[u.position, u.title].filter(Boolean).join(' / ') || '-'}</td>
                    <td className="px-2 py-2 text-slate-500">{u.lastLoginAt ? u.lastLoginAt.replace('T', ' ').slice(0, 16) : '-'}</td>
                    <td className="px-2 py-2">
                      <button onClick={() => toggleUseYn(u)} disabled={u.companyId === 'SYSTEM'}
                        className={`px-2 py-1 rounded text-[11px] font-bold ${u.useYn === 'Y' ? 'bg-emerald-500/15 text-emerald-400' : 'bg-slate-700 text-slate-400'} ${u.companyId === 'SYSTEM' ? 'opacity-40 cursor-not-allowed' : 'cursor-pointer'}`}>
                        {u.useYn === 'Y' ? '활성 Y' : '비활성 N'}
                      </button>
                    </td>
                  </tr>
                ))}
                {users.length === 0 && <tr><td colSpan={8} className="px-2 py-6 text-center text-slate-600">데이터 없음</td></tr>}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {tab === 'history' && (
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-3">
          <div className="flex gap-2 flex-wrap">
            <select value={histCompanyId} onChange={e => setHistCompanyId(e.target.value)}
              className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-slate-200">
              <option value="">전체 회사</option>
              {companies.map(c => <option key={c.id} value={c.id}>{c.id} ({c.name})</option>)}
            </select>
            <input value={histUserId} onChange={e => setHistUserId(e.target.value)} placeholder="아이디(선택)"
              className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-slate-200" />
            <button onClick={fetchHistory} className="px-3 py-1.5 rounded-lg text-xs font-semibold bg-blue-600 text-white">조회</button>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-xs text-slate-300">
              <thead className="text-slate-500 border-b border-slate-800">
                <tr>{['회사', '아이디', '로그인시각', 'IP', '결과'].map(h => <th key={h} className="text-left px-2 py-2 font-semibold">{h}</th>)}</tr>
              </thead>
              <tbody>
                {history.map((h, i) => (
                  <tr key={i} className="border-b border-slate-800/60">
                    <td className="px-2 py-2 font-mono">{h.companyId}</td>
                    <td className="px-2 py-2 font-mono">{h.userId}</td>
                    <td className="px-2 py-2 text-slate-400">{h.loginAt ? h.loginAt.replace('T', ' ').slice(0, 19) : '-'}</td>
                    <td className="px-2 py-2 font-mono text-slate-500">{h.loginIp || '-'}</td>
                    <td className="px-2 py-2">
                      <span className={`px-2 py-0.5 rounded text-[11px] font-bold ${h.loginResult === 'SUCCESS' ? 'bg-emerald-500/15 text-emerald-400' : 'bg-red-500/15 text-red-400'}`}>{h.loginResult}</span>
                    </td>
                  </tr>
                ))}
                {history.length === 0 && <tr><td colSpan={5} className="px-2 py-6 text-center text-slate-600">데이터 없음</td></tr>}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
