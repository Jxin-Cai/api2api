import { Typography } from 'antd';
import { useNavigate } from 'react-router-dom';
import { ROUTE_PATHS } from '@shared/config/constants';
import { getProtocolMeta } from '@shared/lib/protocols';

interface ProtocolMetadataLinkProps {
  protocolType: string;
  label?: string;
}

export function ProtocolMetadataLink({ protocolType, label }: ProtocolMetadataLinkProps) {
  const navigate = useNavigate();
  const meta = getProtocolMeta(protocolType);
  const displayLabel = label ?? `查看 ${meta.label} 字段定义`;

  function handleClick(): void {
    navigate(`${ROUTE_PATHS.adminProtocolMetadata}?protocol=${encodeURIComponent(protocolType)}`);
  }

  return <Typography.Link onClick={handleClick}>{displayLabel}</Typography.Link>;
}
