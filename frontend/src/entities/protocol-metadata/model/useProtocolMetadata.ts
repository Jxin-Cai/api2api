import { useQuery } from '@tanstack/react-query';
import { getProtocolMetadataDetail, listProtocolMetadata } from '../api/protocolMetadataApi';

export const protocolMetadataQueryKeys = {
  all: ['protocol-metadata'] as const,
  detail: (protocolType: string | null) => ['protocol-metadata', 'detail', protocolType] as const,
};

export function useProtocolMetadataList() {
  const query = useQuery({ queryKey: protocolMetadataQueryKeys.all, queryFn: listProtocolMetadata });
  const protocols = query.data?.data.protocols ?? [];
  return { ...query, protocols };
}

export function useProtocolMetadataDetail(protocolType: string | null, enabled = true) {
  return useQuery({
    queryKey: protocolMetadataQueryKeys.detail(protocolType),
    queryFn: () => getProtocolMetadataDetail(protocolType ?? ''),
    enabled: enabled && protocolType !== null,
  });
}
