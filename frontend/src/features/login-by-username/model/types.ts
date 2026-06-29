import type { LoginResponse } from '@entities/user-account';

export interface LoginRequest {
  username: string;
}

export interface LoginFormValues {
  username: string;
}

export interface LoginFormProps {
  onSuccess?: (response: LoginResponse) => void;
}
