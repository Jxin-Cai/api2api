import { useQuery, type UseQueryResult } from '@tanstack/react-query';

import { getAdminDashboard, getFrontDashboard } from '../api/dashboardMetricApi';
import type {
  AdminDashboardResponse,
  FrontDashboardResponse,
  GetAdminDashboardRequest,
  GetFrontDashboardRequest,
} from './types';

export const DASHBOARD_METRICS_QUERY_KEY = 'dashboardMetrics';

export function useFrontDashboardMetrics(
  params: GetFrontDashboardRequest
): UseQueryResult<FrontDashboardResponse> {
  return useQuery({
    queryKey: [DASHBOARD_METRICS_QUERY_KEY, 'front', params],
    queryFn: async (): Promise<FrontDashboardResponse> => {
      const response = await getFrontDashboard(params);
      return response.data;
    },
  });
}

export function useAdminDashboardMetrics(
  params: GetAdminDashboardRequest
): UseQueryResult<AdminDashboardResponse> {
  return useQuery({
    queryKey: [DASHBOARD_METRICS_QUERY_KEY, 'admin', params],
    queryFn: async (): Promise<AdminDashboardResponse> => {
      const response = await getAdminDashboard(params);
      return response.data;
    },
  });
}
