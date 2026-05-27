import { useAuthStore } from '../store/useAuthStore';

interface PrintHeaderProps {
  title: string;
  subtitle?: string;
}

/**
 * 인쇄 전용 공통 헤더 (8단계 통일). 화면에선 숨김(print:block).
 * 회사/출력자/출력일시를 일관된 형식으로 표기.
 */
export default function PrintHeader({ title, subtitle }: PrintHeaderProps) {
  const user = useAuthStore((s) => s.user);
  const now = new Date().toLocaleString('ko-KR');

  return (
    <div className="hidden print:block mb-5">
      <div className="flex justify-between items-end border-b-2 border-slate-800 pb-3 text-slate-800">
        <div>
          <h2 className="text-2xl font-bold tracking-tight">{title}</h2>
          {subtitle && <p className="text-xs text-slate-600 mt-0.5">{subtitle}</p>}
        </div>
        <div className="text-right text-[10px] space-y-0.5 text-slate-600 font-semibold">
          <div>회사: <span className="text-slate-900 font-bold">{user?.companyId || 'CMMS'}</span></div>
          <div>출력자: <span className="text-slate-900 font-bold">{user?.name || '-'}</span></div>
          <div>출력일시: <span className="text-slate-900 font-bold">{now}</span></div>
        </div>
      </div>
    </div>
  );
}
