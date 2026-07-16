import { Modal } from 'antd';
import { ProtocolMetadataDetailPanel } from './ProtocolMetadataDetailPanel';
import './ProtocolMetadataDetailModal.css';

interface ProtocolMetadataDetailModalProps {
  protocolType: string | null;
  open: boolean;
  onClose: () => void;
}

export function ProtocolMetadataDetailModal({
  protocolType,
  open,
  onClose,
}: ProtocolMetadataDetailModalProps) {
  return (
    <Modal
      className="protocol-metadata-modal"
      title="协议规范"
      open={open}
      onCancel={onClose}
      footer={null}
      width={1120}
      centered
      destroyOnClose
      keyboard
      maskClosable
    >
      {protocolType ? <ProtocolMetadataDetailPanel protocolType={protocolType} /> : null}
    </Modal>
  );
}
