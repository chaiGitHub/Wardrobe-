import React, { useState, useEffect } from 'react';
import {
  Smartphone, Download, CheckCircle, ExternalLink,
  X, Sparkles, Layers, ShieldCheck, HelpCircle, ArrowRight, ArrowLeft
} from 'lucide-react';

interface AndroidInstallModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const AndroidInstallModal: React.FC<AndroidInstallModalProps> = ({ isOpen, onClose }) => {
  const [deferredPrompt, setDeferredPrompt] = useState<any>(null);
  const [isInstalled, setIsInstalled] = useState(false);
  const [activeTab, setActiveTab] = useState<'direct-phone' | 'capacitor-zip'>('direct-phone');
  const [showManualNotice, setShowManualNotice] = useState(false);

  useEffect(() => {
    // Check if already in standalone mode
    if (window.matchMedia('(display-mode: standalone)').matches || (window.navigator as any).standalone) {
      setIsInstalled(true);
    }

    const handler = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e);
    };

    window.addEventListener('beforeinstallprompt', handler);
    window.addEventListener('appinstalled', () => {
      setIsInstalled(true);
      setDeferredPrompt(null);
    });

    return () => {
      window.removeEventListener('beforeinstallprompt', handler);
    };
  }, []);

  if (!isOpen) return null;

  const handleInstallClick = async () => {
    if (deferredPrompt) {
      deferredPrompt.prompt();
      const choiceResult = await deferredPrompt.userChoice;
      if (choiceResult.outcome === 'accepted') {
        setIsInstalled(true);
      }
      setDeferredPrompt(null);
    } else {
      setShowManualNotice(true);
    }
  };

  return (
    <div
      id="android-install-fullscreen"
      className="fixed inset-0 z-50 w-full max-w-full h-full bg-white overflow-hidden flex flex-col animate-in fade-in duration-150 pointer-events-auto"
    >
      {/* Full-Screen Top Header */}
      <div className="px-3 sm:px-6 py-2.5 sm:py-3 bg-linear-to-r from-purple-700 via-indigo-700 to-purple-800 text-white flex items-center justify-between shrink-0 shadow-xs w-full max-w-full overflow-x-hidden">
        <div className="flex items-center gap-2 sm:gap-3 min-w-0">
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 -ml-1 sm:ml-0 rounded-xl text-white/80 hover:text-white hover:bg-white/10 transition-colors shrink-0"
            title="Back"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div className="p-2 rounded-xl bg-white/10 backdrop-blur-md border border-white/20 shrink-0">
            <Smartphone className="w-4 h-4 sm:w-5 sm:h-5 text-emerald-300" />
          </div>
          <div className="min-w-0">
            <h2 className="text-sm sm:text-base font-bold text-white truncate">
              Android 手机安装中心
            </h2>
            <p className="text-[10px] sm:text-xs text-purple-200 truncate hidden xs:block">
              专为 Motorola Edge 30 及 Android 设备定制
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="p-1.5 text-white/80 hover:text-white hover:bg-white/10 rounded-xl transition-colors cursor-pointer shrink-0"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

        {/* Tab switch */}
        <div className="flex border-b border-slate-100 bg-slate-50/80 p-1.5 gap-1.5 text-xs font-bold w-full max-w-full overflow-x-hidden">
          <button
            type="button"
            onClick={() => setActiveTab('direct-phone')}
            className={`flex-1 py-2 px-3 rounded-xl transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
              activeTab === 'direct-phone'
                ? 'bg-white text-purple-900 shadow-xs'
                : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            <Sparkles className="w-3.5 h-3.5 text-purple-600" />
            <span>手机直接安装 (最推荐)</span>
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('capacitor-zip')}
            className={`flex-1 py-2 px-3 rounded-xl transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
              activeTab === 'capacitor-zip'
                ? 'bg-white text-purple-900 shadow-xs'
                : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            <Download className="w-3.5 h-3.5 text-indigo-600" />
            <span>Capacitor 原生工程包</span>
          </button>
        </div>

        {/* Content body */}
        <div className="p-4 sm:p-6 overflow-y-auto overflow-x-hidden flex-1 space-y-4 w-full max-w-full">
          {activeTab === 'direct-phone' ? (
            <div className="space-y-4">
              <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 flex items-start gap-3">
                <CheckCircle className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
                <div className="text-xs text-emerald-950 space-y-1">
                  <p className="font-bold text-emerald-900 text-sm">
                    {isInstalled ? '✅ 应用已作为独立 App 安装！' : '⚡ 极速 1 秒安装至 Moto Edge 30 桌面'}
                  </p>
                  <p className="text-emerald-800 leading-relaxed">
                    在 Moto Edge 30 上直接安装为原生 Android App（由 Google Play 服务自动编译 WebAPK），免去任何电脑连线与未知来源安全警告。
                  </p>
                </div>
              </div>

              {/* Install button */}
              <div className="text-center pt-1">
                <button
                  type="button"
                  onClick={handleInstallClick}
                  className="w-full py-3.5 px-6 rounded-2xl bg-linear-to-r from-purple-600 to-indigo-600 hover:from-purple-700 hover:to-indigo-700 text-white font-bold text-sm shadow-md hover:shadow-lg transition-all active:scale-[0.98] cursor-pointer flex items-center justify-center gap-2"
                >
                  <Smartphone className="w-5 h-5" />
                  <span>{deferredPrompt ? '立即一键安装到 Moto Edge 30' : '安装到手机桌面 (添加到主屏幕)'}</span>
                </button>
                {showManualNotice && (
                  <div className="mt-3 p-3 rounded-xl bg-purple-50 border border-purple-200 text-xs text-purple-900 text-left space-y-1">
                    <p className="font-bold">📱 浏览器直接安装指引：</p>
                    <p>1. 点击浏览器右上角「三个点」菜单按钮 (⋮)</p>
                    <p>2. 点击「添加到主屏幕」或「安装应用 (Install App)」</p>
                    <p>系统将自动生成原生 Android App 图标到您的手机桌面！</p>
                  </div>
                )}
              </div>

              {/* Step by step guide */}
              <div className="space-y-2.5 pt-2">
                <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                  Moto Edge 30 安装步骤说明：
                </h4>
                <div className="space-y-2 text-xs text-slate-600">
                  <div className="flex items-start gap-2.5 p-2.5 rounded-xl bg-slate-50 border border-slate-200/70">
                    <span className="w-5 h-5 rounded-full bg-purple-100 text-purple-700 font-bold flex items-center justify-center shrink-0 text-[11px]">
                      1
                    </span>
                    <p className="leading-relaxed">
                      用手机 <strong>Chrome 浏览器</strong> 打开当前网页地址。
                    </p>
                  </div>
                  <div className="flex items-start gap-2.5 p-2.5 rounded-xl bg-slate-50 border border-slate-200/70">
                    <span className="w-5 h-5 rounded-full bg-purple-100 text-purple-700 font-bold flex items-center justify-center shrink-0 text-[11px]">
                      2
                    </span>
                    <p className="leading-relaxed">
                      点击上方按钮，或点击 Chrome 右上角 <strong>「三个点 (⋮)」</strong> 菜单中的 <strong>「安装应用」</strong> 或 <strong>「添加到主屏幕」</strong>。
                    </p>
                  </div>
                  <div className="flex items-start gap-2.5 p-2.5 rounded-xl bg-slate-50 border border-slate-200/70">
                    <span className="w-5 h-5 rounded-full bg-purple-100 text-purple-700 font-bold flex items-center justify-center shrink-0 text-[11px]">
                      3
                    </span>
                    <p className="leading-relaxed">
                      手机桌面即刻生成 <strong>Wardrobe & Wear Journal</strong> 原生应用图标，点击即可全屏无边框运行，支持拍照、离线及云端同步。
                    </p>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            <div className="space-y-4">
              <div className="p-4 rounded-2xl bg-indigo-50 border border-indigo-200 flex items-start gap-3">
                <Layers className="w-5 h-5 text-indigo-600 shrink-0 mt-0.5" />
                <div className="text-xs text-indigo-950 space-y-1">
                  <p className="font-bold text-indigo-900 text-sm">
                    Capacitor 原生 Android 工程已就绪
                  </p>
                  <p className="text-indigo-800 leading-relaxed">
                    已为您生成完整的 Capacitor Android 项目架构（包含 <code>android/</code> 源码、Gradle 构建脚本、AndroidManifest 配置及应用图标）。
                  </p>
                </div>
              </div>

              <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-800">
                    完整 Capacitor Android 工程压缩包
                  </span>
                  <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-purple-100 text-purple-800 font-bold">
                    ZIP 格式
                  </span>
                </div>
                <p className="text-xs text-slate-500">
                  包含完整的 Android Studio 项目，解压后可直接用 Android Studio 打开或通过命令行打包 debug APK。
                </p>
                <a
                  href="/api/android/download-project"
                  download="wardrobe-moto-edge-30-android.zip"
                  className="w-full py-2.5 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs shadow-xs transition-all flex items-center justify-center gap-2 cursor-pointer"
                >
                  <Download className="w-4 h-4" />
                  <span>下载完整 Android Capacitor 工程包 (ZIP)</span>
                </a>
              </div>

              <div className="space-y-2 pt-1">
                <h4 className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                  通过电脑为 Moto Edge 30 打包 APK 步骤：
                </h4>
                <ol className="list-decimal list-inside space-y-1.5 text-xs text-slate-600 leading-relaxed bg-slate-50 p-3.5 rounded-xl border border-slate-200">
                  <li>解压下载的 ZIP 压缩包；</li>
                  <li>用 <strong>Android Studio</strong> 打开 <code>android</code> 文件夹；</li>
                  <li>Moto Edge 30 开启 <strong>USB 调试</strong>，连接电脑；</li>
                  <li>在 Android Studio 点击 <strong>Run (运行)</strong> 绿色三角，即可自动编译并直接安装到您的 Moto Edge 30 手机！</li>
                </ol>
              </div>
            </div>
          )}
        </div>

        {/* Full-Screen Sticky Footer */}
        <div className="px-4 sm:px-6 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] bg-slate-50 border-t border-slate-200/80 flex items-center justify-between text-xs text-slate-500 shrink-0 w-full max-w-full shadow-xs">
          <div className="flex items-center gap-1.5 truncate">
            <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
            <span className="truncate">适配 Motorola Edge 30 (Android 12/13/14)</span>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold transition-colors cursor-pointer shadow-xs active:scale-95 shrink-0"
          >
            完成
          </button>
        </div>
      </div>
  );
};
