import { FrameStats, ConnectionStatus, ProcessingAlgorithm } from './types.js';

/**
 * Statistics manager for tracking and displaying frame processing statistics
 */
export class StatsManager {
    private stats: FrameStats;
    private frameCountElement: HTMLElement;
    private fpsElement: HTMLElement;
    private resolutionElement: HTMLElement;
    private processingTimeElement: HTMLElement;
    private statusTextElement: HTMLElement;
    private statusIndicatorElement: HTMLElement;
    private algorithmTextElement: HTMLElement;
    private formatTextElement: HTMLElement;
    private lastUpdateElement: HTMLElement;
    
    // FPS calculation
    private frameTimestamps: number[] = [];
    private readonly FPS_SAMPLE_SIZE = 30;

    constructor() {
        this.stats = {
            fps: 0,
            width: 640,
            height: 480,
            frameCount: 0,
            processingTime: 0,
            lastUpdate: new Date()
        };

        // Get DOM elements
        this.frameCountElement = this.getElementById('frameCountDisplay');
        this.fpsElement = this.getElementById('fpsDisplay');
        this.resolutionElement = this.getElementById('resolutionDisplay');
        this.processingTimeElement = this.getElementById('processingTimeDisplay');
        this.statusTextElement = this.getElementById('statusText');
        this.statusIndicatorElement = this.getElementById('statusIndicator');
        this.algorithmTextElement = this.getElementById('algorithmText');
        this.formatTextElement = this.getElementById('formatText');
        this.lastUpdateElement = this.getElementById('lastUpdateText');

        // Initialize display
        this.updateDisplay();
    }

    private getElementById(id: string): HTMLElement {
        const element = document.getElementById(id);
        if (!element) {
            throw new Error(`Element with ID '${id}' not found`);
        }
        return element;
    }

    /**
     * Update statistics with new frame data
     */
    public updateFrameStats(width: number, height: number, processingTime: number = 0): void {
        const now = Date.now();
        
        // Update frame count
        this.stats.frameCount++;
        
        // Update resolution
        this.stats.width = width;
        this.stats.height = height;
        
        // Update processing time
        this.stats.processingTime = processingTime;
        
        // Update last update time
        this.stats.lastUpdate = new Date();
        
        // Calculate FPS
        this.frameTimestamps.push(now);
        
        // Keep only recent timestamps for FPS calculation
        if (this.frameTimestamps.length > this.FPS_SAMPLE_SIZE) {
            this.frameTimestamps.shift();
        }
        
        // Calculate FPS if we have enough samples
        if (this.frameTimestamps.length >= 2) {
            const timeDiff = (this.frameTimestamps[this.frameTimestamps.length - 1] - 
                             this.frameTimestamps[0]) / 1000; // Convert to seconds
            this.stats.fps = (this.frameTimestamps.length - 1) / timeDiff;
        }
        
        this.updateDisplay();
    }

    /**
     * Update connection status
     */
    public updateConnectionStatus(status: ConnectionStatus, message?: string): void {
        this.statusIndicatorElement.className = `status-indicator status-${status}`;
        this.statusTextElement.textContent = message || this.getStatusText(status);
    }

    /**
     * Update processing algorithm display
     */
    public updateAlgorithm(algorithm: ProcessingAlgorithm): void {
        this.algorithmTextElement.textContent = algorithm;
    }

    /**
     * Update frame format display
     */
    public updateFormat(format: string): void {
        this.formatTextElement.textContent = format.charAt(0).toUpperCase() + format.slice(1);
    }

    /**
     * Reset statistics
     */
    public resetStats(): void {
        this.stats.frameCount = 0;
        this.stats.fps = 0;
        this.stats.processingTime = 0;
        this.frameTimestamps = [];
        this.updateDisplay();
    }

    /**
     * Get current statistics
     */
    public getStats(): FrameStats {
        return { ...this.stats };
    }

    /**
     * Update the display with current statistics
     */
    private updateDisplay(): void {
        this.frameCountElement.textContent = this.stats.frameCount.toString();
        this.fpsElement.textContent = this.stats.fps.toFixed(1);
        this.resolutionElement.textContent = `${this.stats.width}x${this.stats.height}`;
        this.processingTimeElement.textContent = `${this.stats.processingTime.toFixed(1)}ms`;
        
        // Format last update time
        const now = new Date();
        const diffMs = now.getTime() - this.stats.lastUpdate.getTime();
        this.lastUpdateElement.textContent = this.formatTimeDifference(diffMs);
    }

    /**
     * Format time difference for display
     */
    private formatTimeDifference(diffMs: number): string {
        if (diffMs < 1000) {
            return 'Just now';
        } else if (diffMs < 60000) {
            return `${Math.floor(diffMs / 1000)}s ago`;
        } else if (diffMs < 3600000) {
            return `${Math.floor(diffMs / 60000)}m ago`;
        } else {
            return `${Math.floor(diffMs / 3600000)}h ago`;
        }
    }

    /**
     * Get status text for connection status
     */
    private getStatusText(status: ConnectionStatus): string {
        switch (status) {
            case ConnectionStatus.CONNECTED:
                return 'Connected';
            case ConnectionStatus.DISCONNECTED:
                return 'Disconnected';
            case ConnectionStatus.CONNECTING:
                return 'Connecting...';
            case ConnectionStatus.ERROR:
                return 'Error';
            default:
                return 'Unknown';
        }
    }

    /**
     * Update frame info overlay
     */
    public updateFrameInfo(width: number, height: number, algorithm: ProcessingAlgorithm): void {
        const frameInfoElement = document.getElementById('frameInfo');
        if (frameInfoElement) {
            frameInfoElement.textContent = `${width}x${height} | ${algorithm} | FPS: ${this.stats.fps.toFixed(1)}`;
        }
    }

    /**
     * Start periodic display updates
     */
    public startPeriodicUpdates(intervalMs: number = 1000): void {
        setInterval(() => {
            this.updateDisplay();
        }, intervalMs);
    }
}