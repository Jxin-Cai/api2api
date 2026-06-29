import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  disableProtocolConversion,
  enableProtocolConversion,
  getProtocolConversionByDirection,
  protocolConversionQueryKeys,
} from '@entities/protocol-conversion';
import type { ProtocolDirectionQuery } from './types';

export function useProtocolConversionMutations() {
  const queryClient = useQueryClient();
  function invalidate(): Promise<void> {
    return queryClient.invalidateQueries({ queryKey: protocolConversionQueryKeys.all });
  }

  const enableMutation = useMutation({ mutationFn: enableProtocolConversion, onSuccess: invalidate });
  const disableMutation = useMutation({ mutationFn: disableProtocolConversion, onSuccess: invalidate });
  const directionMutation = useMutation({ mutationFn: (params: ProtocolDirectionQuery) => getProtocolConversionByDirection(params) });

  return { enableMutation, disableMutation, directionMutation, invalidate };
}
