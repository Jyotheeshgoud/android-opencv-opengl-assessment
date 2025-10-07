import { FrameData, ProcessingAlgorithm } from './types.js';

/**
 * Canvas-based frame renderer for displaying processed frames
 */
export class FrameRenderer {
    private canvas: HTMLCanvasElement;
    private ctx: CanvasRenderingContext2D;
    private currentFrame: FrameData | null = null;

    constructor(canvasElement: HTMLCanvasElement) {
        this.canvas = canvasElement;
        const context = this.canvas.getContext('2d');
        if (!context) {
            throw new Error('Failed to get 2D rendering context');
        }
        this.ctx = context;
        
        // Set initial canvas size
        this.canvas.width = 640;
        this.canvas.height = 480;
        
        this.clearFrame();
    }

    /**
     * Render a frame to the canvas
     */
    public renderFrame(frame: FrameData): void {
        this.currentFrame = frame;
        
        // Resize canvas if needed
        if (this.canvas.width !== frame.width || this.canvas.height !== frame.height) {
            this.canvas.width = frame.width;
            this.canvas.height = frame.height;
        }

        try {
            if (typeof frame.data === 'string') {
                // Handle base64 data
                this.renderBase64Frame(frame.data);
            } else {
                // Handle binary data
                this.renderBinaryFrame(frame);
            }
        } catch (error) {
            console.error('Error rendering frame:', error);
            this.showError('Failed to render frame');
        }
    }

    /**
     * Render frame from base64 data
     */
    private renderBase64Frame(base64Data: string): void {
        const img = new Image();
        img.onload = () => {
            this.ctx.drawImage(img, 0, 0, this.canvas.width, this.canvas.height);
        };
        img.onerror = () => {
            this.showError('Failed to load base64 image');
        };
        img.src = `data:image/png;base64,${base64Data}`;
    }

    /**
     * Render frame from binary data
     */
    private renderBinaryFrame(frame: FrameData): void {
        const data = frame.data as Uint8Array;
        const imageData = this.ctx.createImageData(frame.width, frame.height);
        
        if (frame.format === 'grayscale') {
            // Convert grayscale to RGBA
            for (let i = 0; i < data.length; i++) {
                const pixelIndex = i * 4;
                const grayValue = data[i];
                
                imageData.data[pixelIndex] = grayValue;     // Red
                imageData.data[pixelIndex + 1] = grayValue; // Green
                imageData.data[pixelIndex + 2] = grayValue; // Blue
                imageData.data[pixelIndex + 3] = 255;       // Alpha
            }
        } else if (frame.format === 'rgb') {
            // Convert RGB to RGBA
            for (let i = 0; i < data.length; i += 3) {
                const pixelIndex = (i / 3) * 4;
                
                imageData.data[pixelIndex] = data[i];       // Red
                imageData.data[pixelIndex + 1] = data[i + 1]; // Green
                imageData.data[pixelIndex + 2] = data[i + 2]; // Blue
                imageData.data[pixelIndex + 3] = 255;       // Alpha
            }
        } else if (frame.format === 'rgba') {
            // Direct copy
            imageData.data.set(data);
        }
        
        this.ctx.putImageData(imageData, 0, 0);
    }

    /**
     * Generate a sample edge-detected frame for demonstration
     */
    public generateSampleFrame(): void {
        const width = 640;
        const height = 480;
        
        this.canvas.width = width;
        this.canvas.height = height;
        
        // Create edge-like pattern
        const imageData = this.ctx.createImageData(width, height);
        
        for (let y = 0; y < height; y++) {
            for (let x = 0; x < width; x++) {
                const index = (y * width + x) * 4;
                
                // Create edge pattern
                let intensity = 0;
                
                // Horizontal edges
                if (Math.abs(Math.sin(y / 20)) > 0.9) {
                    intensity = 255;
                }
                
                // Vertical edges
                if (Math.abs(Math.sin(x / 30)) > 0.85) {
                    intensity = Math.max(intensity, 200);
                }
                
                // Circular patterns
                const centerX = width / 2;
                const centerY = height / 2;
                const distance = Math.sqrt((x - centerX) ** 2 + (y - centerY) ** 2);
                if (Math.abs(Math.sin(distance / 40)) > 0.8) {
                    intensity = Math.max(intensity, 150);
                }
                
                // Add some noise
                intensity += (Math.random() - 0.5) * 30;
                intensity = Math.max(0, Math.min(255, intensity));
                
                imageData.data[index] = intensity;     // Red
                imageData.data[index + 1] = intensity; // Green
                imageData.data[index + 2] = intensity; // Blue
                imageData.data[index + 3] = 255;       // Alpha
            }
        }
        
        this.ctx.putImageData(imageData, 0, 0);
        
        // Update current frame info
        this.currentFrame = {
            data: new Uint8Array(width * height),
            width,
            height,
            format: 'grayscale',
            timestamp: Date.now(),
            algorithm: ProcessingAlgorithm.CANNY
        };
    }

    /**
     * Generate a test pattern
     */
    public generateTestPattern(): void {
        const width = 640;
        const height = 480;
        
        this.canvas.width = width;
        this.canvas.height = height;
        
        // Clear canvas
        this.ctx.fillStyle = '#000000';
        this.ctx.fillRect(0, 0, width, height);
        
        // Draw grid pattern
        this.ctx.strokeStyle = '#ffffff';
        this.ctx.lineWidth = 1;
        
        // Vertical lines
        for (let x = 0; x < width; x += 40) {
            this.ctx.beginPath();
            this.ctx.moveTo(x, 0);
            this.ctx.lineTo(x, height);
            this.ctx.stroke();
        }
        
        // Horizontal lines
        for (let y = 0; y < height; y += 40) {
            this.ctx.beginPath();
            this.ctx.moveTo(0, y);
            this.ctx.lineTo(width, y);
            this.ctx.stroke();
        }
        
        // Draw center circle
        this.ctx.beginPath();
        this.ctx.arc(width / 2, height / 2, 100, 0, 2 * Math.PI);
        this.ctx.stroke();
        
        // Draw corner markers
        const markerSize = 20;
        this.ctx.fillStyle = '#ffffff';
        this.ctx.fillRect(0, 0, markerSize, markerSize);
        this.ctx.fillRect(width - markerSize, 0, markerSize, markerSize);
        this.ctx.fillRect(0, height - markerSize, markerSize, markerSize);
        this.ctx.fillRect(width - markerSize, height - markerSize, markerSize, markerSize);
        
        // Add text
        this.ctx.fillStyle = '#ffffff';
        this.ctx.font = '20px Arial';
        this.ctx.textAlign = 'center';
        this.ctx.fillText('Test Pattern - 640x480', width / 2, height / 2 + 6);
    }

    /**
     * Clear the frame display
     */
    public clearFrame(): void {
        this.ctx.fillStyle = '#000000';
        this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
        
        // Show "No Frame" message
        this.ctx.fillStyle = '#666666';
        this.ctx.font = '24px Arial';
        this.ctx.textAlign = 'center';
        this.ctx.fillText('No Frame Loaded', this.canvas.width / 2, this.canvas.height / 2);
        
        this.currentFrame = null;
    }

    /**
     * Show error message
     */
    private showError(message: string): void {
        this.ctx.fillStyle = '#ff0000';
        this.ctx.font = '18px Arial';
        this.ctx.textAlign = 'center';
        this.ctx.fillText(message, this.canvas.width / 2, this.canvas.height / 2);
    }

    /**
     * Get current frame information
     */
    public getCurrentFrame(): FrameData | null {
        return this.currentFrame;
    }

    /**
     * Get canvas dimensions
     */
    public getDimensions(): { width: number; height: number } {
        return {
            width: this.canvas.width,
            height: this.canvas.height
        };
    }
}