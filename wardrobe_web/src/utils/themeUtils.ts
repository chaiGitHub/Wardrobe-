export interface CategoryTheme {
  bg: string;
  text: string;
  border: string;
  badge: string;
  accent: string;
}

export function getCategoryTheme(type: string = ''): CategoryTheme {
  const t = (type || '').toLowerCase();
  if (t.includes('dress')) {
    return {
      bg: 'bg-rose-50',
      text: 'text-rose-700',
      border: 'border-rose-200',
      badge: 'bg-rose-100 text-rose-800 border-rose-300',
      accent: 'text-rose-600'
    };
  }
  if (t.includes('bra')) {
    return {
      bg: 'bg-violet-50',
      text: 'text-violet-700',
      border: 'border-violet-200',
      badge: 'bg-violet-100 text-violet-800 border-violet-300',
      accent: 'text-violet-600'
    };
  }
  if (t.includes('tshirt') || t.includes('t-shirt') || t.includes('top') || t.includes('shirt')) {
    return {
      bg: 'bg-sky-50',
      text: 'text-sky-700',
      border: 'border-sky-200',
      badge: 'bg-sky-100 text-sky-800 border-sky-300',
      accent: 'text-sky-600'
    };
  }
  if (t.includes('pant') || t.includes('trouser') || t.includes('jeans') || t.includes('bottom')) {
    return {
      bg: 'bg-amber-50',
      text: 'text-amber-700',
      border: 'border-amber-200',
      badge: 'bg-amber-100 text-amber-800 border-amber-300',
      accent: 'text-amber-600'
    };
  }
  if (t.includes('skirt')) {
    return {
      bg: 'bg-pink-50',
      text: 'text-pink-700',
      border: 'border-pink-200',
      badge: 'bg-pink-100 text-pink-800 border-pink-300',
      accent: 'text-pink-600'
    };
  }
  if (t.includes('jacket') || t.includes('coat') || t.includes('outerwear')) {
    return {
      bg: 'bg-indigo-50',
      text: 'text-indigo-700',
      border: 'border-indigo-200',
      badge: 'bg-indigo-100 text-indigo-800 border-indigo-300',
      accent: 'text-indigo-600'
    };
  }
  if (t.includes('underwear')) {
    return {
      bg: 'bg-emerald-50',
      text: 'text-emerald-700',
      border: 'border-emerald-200',
      badge: 'bg-emerald-100 text-emerald-800 border-emerald-300',
      accent: 'text-emerald-600'
    };
  }
  return {
    bg: 'bg-purple-50',
    text: 'text-purple-700',
    border: 'border-purple-200',
    badge: 'bg-purple-100 text-purple-800 border-purple-300',
    accent: 'text-purple-600'
  };
}
