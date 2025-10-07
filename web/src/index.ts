import { FrameRenderer } from './frameRenderer.js';
import { StatsManager } from './statsManager.js';
import { FrameData, ProcessingAlgorithm, ConnectionStatus } from './types.js';

/**
 * Main application class for the Edge Detection Web Viewer
 */
class EdgeDetectionViewer {
    private frameRenderer: FrameRenderer;
    private statsManager: StatsManager;
    private isInitialized = false;

    constructor() {
        console.log('🔍 Edge Detection Viewer - Initializing...');
    }

    /**
     * Initialize the application
     */
    public async initialize(): Promise<void> {
        try {
            // Wait for DOM to be ready
            await this.waitForDOM();
            
            // Initialize components
            this.initializeRenderer();
            this.initializeStatsManager();
            this.setupEventListeners();
            
            // Set initial status
            this.statsManager.updateConnectionStatus(ConnectionStatus.DISCONNECTED, 'Ready for frames');
            this.statsManager.updateAlgorithm(ProcessingAlgorithm.CANNY);
            this.statsManager.updateFormat('grayscale');
            
            // Start periodic updates
            this.statsManager.startPeriodicUpdates();
            
            this.isInitialized = true;
            console.log('✅ Edge Detection Viewer initialized successfully');
            
        } catch (error) {
            console.error('❌ Failed to initialize viewer:', error);
            throw error;
        }
    }

    /**
     * Wait for DOM to be ready
     */
    private waitForDOM(): Promise<void> {
        return new Promise((resolve) => {
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', () => resolve());
            } else {
                resolve();
            }
        });
    }

    /**
     * Initialize the frame renderer
     */
    private initializeRenderer(): void {
        const canvas = document.getElementById('frameCanvas') as HTMLCanvasElement;
        if (!canvas) {
            throw new Error('Canvas element not found');
        }
        
        this.frameRenderer = new FrameRenderer(canvas);
        console.log('📹 Frame renderer initialized');
    }

    /**
     * Initialize the statistics manager
     */
    private initializeStatsManager(): void {
        this.statsManager = new StatsManager();
        console.log('📊 Statistics manager initialized');
    }

    /**
     * Set up event listeners for UI controls
     */
    private setupEventListeners(): void {
        // Load sample frame button
        const loadSampleBtn = document.getElementById('loadSampleBtn');
        loadSampleBtn?.addEventListener('click', () => {
            this.loadSampleFrame();
        });

        // Generate test pattern button
        const generatePatternBtn = document.getElementById('generatePatternBtn');
        generatePatternBtn?.addEventListener('click', () => {
            this.generateTestPattern();
        });

        // Clear frame button
        const clearFrameBtn = document.getElementById('clearFrameBtn');
        clearFrameBtn?.addEventListener('click', () => {
            this.clearFrame();
        });

        console.log('🎛️ Event listeners set up');
    }

    /**
     * Load a sample processed frame
     */
    private loadSampleFrame(): void {
        console.log('🖼️ Loading sample frame...');
        
        try {
            // Update button states
            this.updateButtonStates('loadSampleBtn');
            
            // Generate sample frame
            this.frameRenderer.generateSampleFrame();
            
            // Update statistics
            const dimensions = this.frameRenderer.getDimensions();
            this.statsManager.updateFrameStats(dimensions.width, dimensions.height, 15.2);
            this.statsManager.updateConnectionStatus(ConnectionStatus.CONNECTED, 'Sample frame loaded');
            this.statsManager.updateFrameInfo(dimensions.width, dimensions.height, ProcessingAlgorithm.CANNY);
            
            console.log('✅ Sample frame loaded successfully');
            
        } catch (error) {
            console.error('❌ Error loading sample frame:', error);
            this.statsManager.updateConnectionStatus(ConnectionStatus.ERROR, 'Failed to load sample');
        }
    }

    /**
     * Generate test pattern
     */
    private generateTestPattern(): void {
        console.log('🔧 Generating test pattern...');
        
        try {
            // Update button states
            this.updateButtonStates('generatePatternBtn');
            
            // Generate test pattern
            this.frameRenderer.generateTestPattern();
            
            // Update statistics
            const dimensions = this.frameRenderer.getDimensions();
            this.statsManager.updateFrameStats(dimensions.width, dimensions.height, 5.8);
            this.statsManager.updateConnectionStatus(ConnectionStatus.CONNECTED, 'Test pattern generated');
            this.statsManager.updateAlgorithm(ProcessingAlgorithm.RAW);
            this.statsManager.updateFrameInfo(dimensions.width, dimensions.height, ProcessingAlgorithm.RAW);
            
            console.log('✅ Test pattern generated successfully');
            
        } catch (error) {
            console.error('❌ Error generating test pattern:', error);
            this.statsManager.updateConnectionStatus(ConnectionStatus.ERROR, 'Failed to generate pattern');
        }
    }

    /**
     * Clear the current frame
     */
    private clearFrame(): void {
        console.log('🧹 Clearing frame...');
        
        try {
            // Update button states
            this.updateButtonStates('clearFrameBtn');
            
            // Clear frame
            this.frameRenderer.clearFrame();
            
            // Reset statistics
            this.statsManager.resetStats();
            this.statsManager.updateConnectionStatus(ConnectionStatus.DISCONNECTED, 'Frame cleared');
            
            // Clear frame info
            const frameInfoElement = document.getElementById('frameInfo');
            if (frameInfoElement) {
                frameInfoElement.textContent = 'No frame loaded';
            }
            
            console.log('✅ Frame cleared successfully');
            
        } catch (error) {
            console.error('❌ Error clearing frame:', error);
            this.statsManager.updateConnectionStatus(ConnectionStatus.ERROR, 'Failed to clear frame');
        }
    }

    /**
     * Update button active states
     */
    private updateButtonStates(activeButtonId: string): void {
        const buttons = ['loadSampleBtn', 'generatePatternBtn', 'clearFrameBtn'];
        
        buttons.forEach(buttonId => {
            const button = document.getElementById(buttonId);
            if (button) {
                if (buttonId === activeButtonId) {
                    button.classList.add('active');
                } else {
                    button.classList.remove('active');
                }
            }
        });
    }

    /**
     * Process incoming frame data (for future WebSocket/HTTP integration)
     */
    public processIncomingFrame(frameData: FrameData): void {
        if (!this.isInitialized) {
            console.warn('⚠️ Viewer not initialized, ignoring frame');
            return;
        }

        try {
            // Render the frame
            this.frameRenderer.renderFrame(frameData);
            
            // Update statistics
            this.statsManager.updateFrameStats(
                frameData.width, 
                frameData.height, 
                performance.now() - frameData.timestamp
            );
            
            this.statsManager.updateConnectionStatus(ConnectionStatus.CONNECTED, 'Receiving frames');
            this.statsManager.updateAlgorithm(frameData.algorithm);
            this.statsManager.updateFormat(frameData.format);
            this.statsManager.updateFrameInfo(frameData.width, frameData.height, frameData.algorithm);
            
        } catch (error) {
            console.error('❌ Error processing incoming frame:', error);
            this.statsManager.updateConnectionStatus(ConnectionStatus.ERROR, 'Frame processing error');
        }
    }

    /**
     * Get current application status
     */
    public getStatus(): { initialized: boolean; stats: any } {
        return {
            initialized: this.isInitialized,
            stats: this.statsManager.getStats()
        };
    }
}

/**
 * Initialize and start the application
 */
async function main() {
    try {
        const viewer = new EdgeDetectionViewer();
        await viewer.initialize();
        
        // Make viewer available globally for debugging
        (window as any).edgeDetectionViewer = viewer;
        
        console.log('🚀 Edge Detection Viewer ready!');
        
    } catch (error) {
        console.error('💥 Fatal error initializing application:', error);
        
        // Show error message to user
        const errorDiv = document.createElement('div');
        errorDiv.style.cssText = `
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background: #f44336;
            color: white;
            padding: 20px;
            border-radius: 8px;
            text-align: center;
            z-index: 1000;
        `;
        errorDiv.innerHTML = `
            <h3>⚠️ Application Error</h3>
            <p>Failed to initialize the Edge Detection Viewer.</p>
            <p>Please check the console for details.</p>
        `;
        document.body.appendChild(errorDiv);
    }
}

// Start the application
main();