import { useRef, useState } from 'react';
import { ImagePlus } from 'lucide-react';
import { resizeToDataUrl } from '@/lib/image';
import { Button } from './Button';

/** Studio logo picker: shrinks the file in the browser, shows a 48 px preview; `null` = no logo */
export function LogoInput({ value, onChange, disabled }: { value: string | null; onChange: (v: string | null) => void; disabled?: boolean }) {
  const input = useRef<HTMLInputElement>(null);
  const [error, setError] = useState<string | null>(null);

  const pick = async (file: File | undefined) => {
    if (!file) return;
    try {
      setError(null);
      onChange(await resizeToDataUrl(file));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không đọc được ảnh');
    } finally {
      if (input.current) input.current.value = '';
    }
  };

  return (
    <div>
      <div className="flex items-center gap-3">
        <div className="w-12 h-12 rounded-lg border border-slate-200 bg-slate-50 flex items-center justify-center overflow-hidden">
          {value ? <img src={value} alt="Logo studio" className="max-w-full max-h-full object-contain" /> : <ImagePlus size={18} className="text-slate-300" />}
        </div>
        {!disabled && (
          <>
            <input ref={input} type="file" accept="image/png,image/jpeg" className="hidden" onChange={(e) => pick(e.target.files?.[0])} />
            <Button type="button" variant="outline" onClick={() => input.current?.click()}>Chọn ảnh</Button>
            {value && <Button type="button" variant="ghost" onClick={() => onChange(null)}>Xoá</Button>}
          </>
        )}
      </div>
      {error && <p className="text-xs text-red-600 mt-1">{error}</p>}
    </div>
  );
}
