import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getProtocolConversion,
  listProtocolConversions,
} from '../api/protocolConversionApi';

export const protocolConversionQueryKeys = {
  all: ['protocol-conversions'] as const,
  detail: (id: string | number | null) => ['protocol-conversions', 'detail', id] as const,
};

export function useProtocolConversions() {
  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: protocolConversionQueryKeys.all, queryFn: listProtocolConversions });
  const conversions = query.data?.data.conversions ?? [];

  function invalidateProtocolConversions(): Promise<void> {
    return queryClient.invalidateQueries({ queryKey: protocolConversionQueryKeys.all });
  }

  return { ...query, conversions, invalidateProtocolConversions };
}

export function useProtocolConversionDetail(definitionId: string | number | null, enabled: boolean) {
  return useQuery({
    queryKey: protocolConversionQueryKeys.detail(definitionId),
    queryFn: () => getProtocolConversion(definitionId ?? ''),
    enabled: enabled && definitionId !== null,
  });
}
