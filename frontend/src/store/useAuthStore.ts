import { create } from 'zustand';
import axiosInstance from '../api/axios';

interface User {
  companyId: string;
  id: string;
  name: string;
  roleId: string;
  departmentId: string | null;
  position: string | null;
  title: string | null;
}

interface AuthState {
  user: User | null;
  token: string | null;
  timeRemaining: number;
  timerId: any | null;
  error: string | null;
  login: (companyId: string, id: string, password: string) => Promise<boolean>;
  logout: () => void;
  signUp: (data: any) => Promise<void>;
  extendSession: () => Promise<void>;
  updateUser: (data: Partial<User>) => void;
  decrementTimer: () => void;
  setError: (msg: string | null) => void;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  token: null,
  timeRemaining: 0,
  timerId: null,
  error: null,

  login: async (companyId, id, password) => {
    try {
      set({ error: null });
      const response = await axiosInstance.post('/auth/login', { companyId, id, password });
      const data = response.data;
      
      const user: User = {
        companyId: data.companyId,
        id: data.id,
        name: data.name,
        roleId: data.roleId,
        departmentId: data.departmentId,
        position: data.position,
        title: data.title,
      };

      const token = data.accessToken;
      
      axiosInstance.defaults.headers.common['Authorization'] = `Bearer ${token}`;

      if (get().timerId) {
        clearInterval(get().timerId);
      }

      const timerId = setInterval(() => {
        get().decrementTimer();
      }, 1000);

      set({
        user,
        token,
        timeRemaining: 1800,
        timerId,
      });

      return true;
    } catch (err: any) {
      const errMsg = err.response?.data?.message || '로그인에 실패했습니다. 입력 정보를 확인하세요.';
      set({ error: errMsg });
      return false;
    }
  },

  logout: () => {
    if (get().timerId) {
      clearInterval(get().timerId);
    }
    delete axiosInstance.defaults.headers.common['Authorization'];
    set({
      user: null,
      token: null,
      timeRemaining: 0,
      timerId: null,
      error: null,
    });
  },

  signUp: async (signUpData) => {
    try {
      set({ error: null });
      await axiosInstance.post('/auth/signup', signUpData);
    } catch (err: any) {
      const errMsg = err.response?.data?.message || '회원가입에 실패했습니다.';
      set({ error: errMsg });
      throw new Error(errMsg);
    }
  },

  extendSession: async () => {
    const { token } = get();
    if (!token) return;

    try {
      set({ error: null });
      const response = await axiosInstance.post('/auth/refresh', {}, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      const newToken = response.data;

      axiosInstance.defaults.headers.common['Authorization'] = `Bearer ${newToken}`;

      set({
        token: newToken,
        timeRemaining: 1800,
      });
    } catch (err: any) {
      console.error('Session extension failed', err);
      get().logout();
    }
  },

  updateUser: (updatedData) => {
    const { user } = get();
    if (!user) return;
    set({
      user: {
        ...user,
        ...updatedData
      }
    });
  },

  decrementTimer: () => {
    const { timeRemaining } = get();
    if (timeRemaining <= 1) {
      get().logout();
      alert('세션이 만료되어 로그아웃되었습니다.');
    } else {
      set({ timeRemaining: timeRemaining - 1 });
    }
  },

  setError: (msg) => set({ error: msg }),
}));
