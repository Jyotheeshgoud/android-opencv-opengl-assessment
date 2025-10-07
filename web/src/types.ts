/**
 * Frame statistics interface
 */
export interface FrameStats {
    fps: number;
    width: number;
    height: number;
    frameCount: number;
    processingTime: number;
    lastUpdate: Date;
}

/**
 * Processing algorithms enum
 */
export enum ProcessingAlgorithm {
    RAW = 'Raw Feed',
    CANNY = 'Canny Edge Detection',
    GRAYSCALE = 'Grayscale Conversion'
}

/**
 * Frame data interface
 */
export interface FrameData {
    data: Uint8Array | string; // Binary data or base64
    width: number;
    height: number;
    format: 'grayscale' | 'rgb' | 'rgba';
    timestamp: number;
    algorithm: ProcessingAlgorithm;
}

/**
 * Connection status
 */
export enum ConnectionStatus {
    CONNECTED = 'connected',
    DISCONNECTED = 'disconnected',
    CONNECTING = 'connecting',
    ERROR = 'error'
}