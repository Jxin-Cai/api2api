import type { LoginResponse } from '@entities/user-account';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginFormValues {
  username: string;
  password: string;
}

export interface LoginFormProps {
  onSuccess?: (response: LoginResponse) => void;
}
