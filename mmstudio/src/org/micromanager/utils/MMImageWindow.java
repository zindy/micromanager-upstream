///////////////////////////////////////////////////////////////////////////////
//FILE:          MMImageWindow.java
//PROJECT:       Micro-Manager
//SUBSYSTEM:     mmstudio
//-----------------------------------------------------------------------------
//
// AUTHOR:       Nenad Amodaj, nenad@amodaj.com, October 1, 2006
//
// COPYRIGHT:    University of California, San Francisco, 2006
//
// LICENSE:      This file is distributed under the BSD license.
//               License text is included with the source distribution.
//
//               This file is distributed in the hope that it will be useful,
//               but WITHOUT ANY WARRANTY; without even the implied warranty
//               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
//
// CVS:          $Id$
//
package org.micromanager.utils;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Color;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.ColorModel;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.JButton;

import mmcorej.CMMCore;

import org.micromanager.MMStudioMainFrame;

import com.swtdesigner.SwingResourceManager;
import ij.CompositeImage;
import ij.ImageStack;
import java.awt.Graphics;
import org.micromanager.image5d.ChannelDisplayProperties;

/**
 * ImageJ compatible image window. Derived from the original ImageJ class.
 */
public class MMImageWindow extends ImageWindow {
	private static final long serialVersionUID = 1L;
	private static final String WINDOW_X = "mmimg_y";
	private static final String WINDOW_Y = "mmimg_x";
	private static final String WINDOW_WIDTH = "mmimg_width";
	private static final String WINDOW_HEIGHT = "mmimg_height";
	private static CMMCore core_ = null;
	private static String title_ = "Live";
	private static ColorModel currentColorLUT__ = null;
	private static Preferences prefs_ = null;
	private static MMStudioMainFrame gui_ = null;

	private Panel buttonPanel_;
	private static ContrastSettings contrastSettings8_ = new ContrastSettings();
	private static ContrastSettings contrastSettings16_ = new ContrastSettings();;
	private LUTDialog contrastDlg_;
	private ImageController contrastPanel_ = null;


	public MMImageWindow(ImagePlus imp, CMMCore core) throws Exception {
		super(imp);
		core_ = core;
		Initialize();
	}

	public MMImageWindow(CMMCore core, ImageController contrastPanel)
			throws Exception {
		super(createImagePlus(core_ = core, title_));
		contrastPanel_ = contrastPanel;
		core_ = core;
		Initialize();
	}

	public MMImageWindow(CMMCore core, MMStudioMainFrame gui,
			ImageController contrastPanel) throws Exception {
		super(createImagePlus(core_ = core, title_));
		contrastPanel_ = contrastPanel;
		gui_ = gui;
		core_ = core;
		Initialize();
	}

	public MMImageWindow(CMMCore core, ImageController contrastPanel,
			String wndTitle) throws Exception {
		super(createImagePlus(core_ = core, title_ = wndTitle));
		contrastPanel_ = contrastPanel;
		core_ = core;
		Initialize();
	}

	public static void setContrastSettings(ContrastSettings s8,
			ContrastSettings s16) {
		contrastSettings8_ = s8;
		contrastSettings16_ = s16;
	}

	public static void setContrastSettings8(ContrastSettings s8) {
		contrastSettings16_ = s8;
	}

	public static void setContrastSetting16(ContrastSettings s16) {
		contrastSettings16_ = s16;
	}

	public ContrastSettings getCurrentContrastSettings() {
		if (getImagePlus().getBitDepth() == 8)
			return contrastSettings8_;
		else
			return contrastSettings16_;
	}

	public static ContrastSettings getContrastSettings8() {
		return contrastSettings8_;
	}

	public static ContrastSettings getContrastSettings16() {
		return contrastSettings16_;
	}

	public void loadPosition(int x, int y) {
		if (prefs_ != null)
			setLocation(prefs_.getInt(WINDOW_X, x), prefs_.getInt(WINDOW_Y, y));
	}

	public void savePosition() {
		if (prefs_ == null)
			loadPreferences();
		Rectangle r = getBounds();
		// save window position
		prefs_.putInt(WINDOW_X, r.x);
		prefs_.putInt(WINDOW_Y, r.y);
		prefs_.putInt(WINDOW_WIDTH, r.width);
		prefs_.putInt(WINDOW_HEIGHT, r.height);
	}

	private static ImagePlus createImagePlus(CMMCore core, String wndTitle)
			throws Exception {
	   core_ = core;
		ImageProcessor ip = null;
		long byteDepth = core_.getBytesPerPixel();
		long components = core_.getNumberOfComponents();
		int width = (int) core_.getImageWidth();
		int height = (int) core_.getImageHeight();
		if (byteDepth == 0) {
			throw (new Exception(logError("Imaging device not initialized")));
		}
		if (byteDepth == 1 && components == 1) {
			ip = new ByteProcessor(width, height);
			if (contrastSettings8_.getRange() == 0.0)
				ip.setMinAndMax(0, 255);
			else
				ip.setMinAndMax(contrastSettings8_.min, contrastSettings8_.max);
		} else if (byteDepth == 2 && components == 1) {
			ip = new ShortProcessor(width, height);
			if (contrastSettings16_.getRange() == 0.0)
				ip.setMinAndMax(0, 65535);
			else
				ip.setMinAndMax(contrastSettings16_.min,
						contrastSettings16_.max);
		} else if (byteDepth == 4 && components == 4) {
			// RGB32 format
			ip = new ColorProcessor(width, height);
			if (contrastSettings8_.getRange() == 0.0)
				ip.setMinAndMax(0, 255);
			else
				ip.setMinAndMax(contrastSettings8_.min, contrastSettings8_.max);
		} else if (byteDepth == 8 && components == 4) {
			// RGB64 format
			ip = new ColorProcessor(width, height);
			if (contrastSettings8_.getRange() == 0.0){
				// todo get ADC bit depth from camera, 
				int bitdepth = 12;
				ip.setMinAndMax(0, (1<<(bitdepth-1))-1);
			}
			else
				ip.setMinAndMax(contrastSettings8_.min, contrastSettings8_.max);
		}else if (byteDepth == 1 && components == 4) {
			// Note: unify cameras to return byteDepth per x,y pixel, not per x,y,component
			ip = new ColorProcessor(width, height);
			if (contrastSettings8_.getRange() == 0.0)
				ip.setMinAndMax(0, 255);
			else
				ip.setMinAndMax(contrastSettings8_.min, contrastSettings8_.max);
		}else if (byteDepth == 2 && components == 4) {
			// assuming RGB32 format
			ip = new ColorProcessor(width, height);
			//todo get actual dynamic range from camera
			if (contrastSettings8_.getRange() == 0.0){
				// todo get ADC bit depth from camera, 
				int bitdepth = 12;
				ip.setMinAndMax(0, (1<<(bitdepth-1))-1);
			}else
				ip.setMinAndMax(contrastSettings8_.min, contrastSettings8_.max);
		}else {
			String message = "Unsupported pixel depth: "
					+ core_.getBytesPerPixel() + " byte(s) and " + components
					+ " channel(s).";
			throw (new Exception(logError(message)));
		}
		ip.setColor(Color.black);
		if (currentColorLUT__ != null && !(ip instanceof ColorProcessor )) {
			ip.setColorModel(currentColorLUT__);
			logError("Restoring color model:" + currentColorLUT__.toString());
		}
		ip.fill();
		return new ImagePlus(title_ = wndTitle, ip);
	}

	public void Initialize() {

		setIJCal();
		setPreferredLocation();

		buttonPanel_ = new Panel();

		AbstractButton saveButton = new JButton("Save");
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new FileSaver(getImagePlus()).save();
			}
		});
		buttonPanel_.add(saveButton);

		AbstractButton saveAsButton = new JButton("Save As...");
		saveAsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new FileSaver(getImagePlus()).saveAsTiff();
			}
		});
		buttonPanel_.add(saveAsButton);

		AbstractButton addToSeriesButton = new JButton("Add to Series");
		addToSeriesButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					gui_.addToSnapSeries(getImagePlus().getProcessor()
							.getPixels(),null);

				} catch (Exception e2) {
					ReportingUtils.showError(e2);
				}
			}
		});
		buttonPanel_.add(addToSeriesButton);

		add(buttonPanel_);
		pack();

		// add window listeners
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				saveAttributes();
			}
		});
		addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
            if (closed)
               return;
				if (contrastPanel_ != null) {
					contrastPanel_.setImagePlus(null, null, null);
            }

			}
		});

		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				getCanvas().requestFocus();
			}
		});

		addWindowListener(new WindowAdapter() {
			public void windowGainedFocus(WindowEvent e) {
				// updateHistogram();
			}
		});

		addWindowListener(new WindowAdapter() {
			public void windowActivated(WindowEvent e) {
				// updateHistogram();
			}
		});
		setIconImage(SwingResourceManager.getImage(MMStudioMainFrame.class,
				"/org/micromanager/icons/camera_go.png"));

		setIJCal();
	}

	public void saveAttributes() {
		if (contrastDlg_ != null)
			contrastDlg_.dispose();
		try {
			savePosition();
			// ToDo: implement winAccesslock_;
			// remember LUT so that a new window can be opened with the
			// same LUT
			ImagePlus imgp = getImagePlus();
			ImageProcessor ip = imgp != null ? imgp.getProcessor() : null;
			boolean isLUT = ip != null ? ip.isPseudoColorLut() : false;
			if (isLUT) {
				currentColorLUT__ = getImagePlus().getProcessor()
						.getColorModel();
				// !!!
				core_.logMessage(
						"Storing color model:" + currentColorLUT__.toString());
			} else {
				core_.logMessage(
						"Color model was not stored successfully"
								+ (null == currentColorLUT__ ? "null" : currentColorLUT__.toString()) + "ImagePlus:"
								+ imgp == null ? "null"
								: "OK" + "ip:" + ip == null ? "null" : "OK");
			}

		} catch (Exception e) {
			ReportingUtils.showError(e);
		}

	}

	public void windowOpened(WindowEvent e) {
		getCanvas().requestFocus();
	}

	private void loadPreferences() {
		prefs_ = Preferences.userNodeForPackage(this.getClass());
	}

	public void setFirstInstanceLocation() {
		setLocationRelativeTo(getParent());
	}

	public void setPreferredLocation() {
		loadPreferences();
		Point p = getLocation();
		loadPosition(p.x, p.y);
	}

	private static String logError(String message) {
		core_.logMessage("MMImageWindow:" + message);
		return message;
	}

	protected void updateHistogram() {
		if (contrastPanel_ != null) {
			contrastPanel_.setImagePlus(getImagePlus(), contrastSettings8_,
					contrastSettings16_);
		}
	}

	public long getImageWindowByteLength() {
		ImageProcessor ip = getImagePlus().getProcessor();
		int w = ip.getWidth();
		int h = ip.getHeight();
		int imPlusBitDepth = getImagePlus().getBitDepth();
		// ImageWindow returns bitdepth 24 when Image processor type is Color
		imPlusBitDepth = imPlusBitDepth == 24 ? 32 : imPlusBitDepth;
		return  w * h * imPlusBitDepth / 8;
	}

    // this returns the length of the raw data array
	public long imageByteLenth(Object pixels) throws IllegalArgumentException {
		int byteLength = 0;
		if (pixels instanceof byte[]) {
			byte bytePixels[] = (byte[]) pixels;
			byteLength = bytePixels.length;
		} else if (pixels instanceof short[]) {
			short bytePixels[] = (short[]) pixels;
			byteLength = bytePixels.length * 2;
		} else if (pixels instanceof int[]) {
			int bytePixels[] = (int[]) pixels;
			byteLength = bytePixels.length * 4;
		} else
			throw (new IllegalArgumentException("Unsupported pixel data type."));
		return byteLength;
	}

	public boolean windowNeedsResizing() {
		ImageProcessor ip = getImagePlus().getProcessor();
		int w = ip.getWidth();
		int h = ip.getHeight();
		int imPlusBitDepth = getImagePlus().getBitDepth();
		
		// ImageWindow returns bitdepth 24 when Image processor type is Color
        if( 24 == imPlusBitDepth){
            imPlusBitDepth = 32;
        }

   		long components = core_.getNumberOfComponents();
        
        // retrieve the image storage size per pixel,
        // 1 for 8 bit gray, 2 for 16 bit gray, 4 for 32 bit RGB, 8 for 64 bit RGB
        long coreTotalByteDepth = core_.getBytesPerPixel();

		// warn the user if image dimensions do not match the current window
		boolean ret = w != core_.getImageWidth() || h != core_.getImageHeight()
				|| (imPlusBitDepth != coreTotalByteDepth * 8 && ( 4==components && (imPlusBitDepth*4 != coreTotalByteDepth * 8))) ;
		return ret;
	}

    public void newImage(Object img) {
        
        long ibd = core_.getImageBitDepth();
        long bpp = core_.getBytesPerPixel();
        long noc = core_.getNumberOfComponents();
        long ncc = core_.getNumberOfCameraChannels();

        // flag to check for 64 bit color
        boolean deepColor = (1 < noc) && (4 < bpp);

        ImagePlus ip = getImagePlus();
        ImageProcessor ipr = ip.getProcessor();

        ImagePlus compositeImage;
        ImagePlus iplus;

        
        
        if (null != ip) {
            if(!deepColor){
                ip.setTitle(title_);
                if (null != ipr) {
                    ipr.setPixels(img);
                }
            }
            else  { // convert to stack
                int w00 = ipr.getWidth();
                int h00 = ipr.getHeight();
                //Image5D img5d = new Image5D(title_, ImagePlus.GRAY16, w00, h00, 3, 1, 1, false);

                short [] R = new short[w00 * h00];
                short [] G = new short[w00 * h00];
                short [] B = new short[w00 * h00];

                // display 64 bit color images as a composite.
                int [] pixels = (int[])img;

                int ii = 0;
                for(int ij0 = 0; ij0 < w00*h00; ++ij0)                {
                        B[ij0] = (short)(pixels[ii]&0xffff);
                        G[ij0] = (short)(pixels[ii++]>>16);
                        R[ij0] = (short)(pixels[ii++]&0xffff);
                }


                    iplus = new ImagePlus( );
                    ImageProcessor imageProcessor = new ShortProcessor(w00, h00);
                    ImageStack imageStack = new ImageStack(imageProcessor.getWidth(), imageProcessor.getHeight(), imageProcessor.getColorModel());

                    imageProcessor.setPixels(R);
                    imageProcessor.setColorModel(ChannelDisplayProperties.createModelFromColor(Color.red));
                    imageStack.addSlice("Red", imageProcessor);

                    imageProcessor.setPixels(G);
                    imageProcessor.setColorModel(ChannelDisplayProperties.createModelFromColor(Color.green));
                    imageStack.addSlice("Green", imageProcessor);

                    iplus.setPosition(3,1,1);
                    imageProcessor.setPixels(B);
                    imageProcessor.setColorModel(ChannelDisplayProperties.createModelFromColor(Color.blue));
                    imageStack.addSlice("Blue", imageProcessor);

                    iplus.setStack(imageStack);

                    compositeImage = new CompositeImage(iplus, CompositeImage.COMPOSITE);

                    //set the Window's image to the composite image
                    // note: only one channel at a time subsequently can be selected with arrow keys
                    setImage(compositeImage);
/*                }else{  // re-use the existing composite image!!

                    ImageStack imageStack = compositeImage.getStack();
                    ImageProcessor p = imageStack.getProcessor(1);
                    p.setPixels(R);
                    p = imageStack.getProcessor(2);
                    p.setPixels(G);
                    p = imageStack.getProcessor(3);
                    p.setPixels(B);
                    setImage(compositeImage);
                    getImagePlus().updateAndDraw();
                }

 */

            }


           updateHistogram();
           tearFreeUpdate();

            // update coordinate and pixel info in imageJ by simulating mouse
            // move
            ImageCanvas ica = getCanvas();
            if (null != ica) {
                Point pt = ica.getCursorLoc();
                ip.mouseMoved(pt.x, pt.y);
            }
        }
    }

	public void newImageWithStatusLine(Object img, String statusLine) {
      //TODO: add error handling
		if (getImageWindowByteLength() != imageByteLenth(img)) {
			throw (new RuntimeException("Image bytelenth does not much"));
		}
		ImagePlus ip = getImagePlus();
		if(null != ip) {
         ip.setTitle(statusLine);
			ImageProcessor ipr = ip.getProcessor();
			if(null != ipr){
				ipr.setPixels(img);
			}
			// ip.updateAndDraw();
 
			updateHistogram();

			// update coordinate and pixel info in imageJ by simulating mouse
			// move
			ImageCanvas ic = getCanvas();
			if(null != ic)
			{
				Point pt = ic.getCursorLoc();
				ip.mouseMoved(pt.x, pt.y);
			}
		}
	}
	
	public void displayStatusLine(String statusLine) {
	   ImagePlus ip = getImagePlus();
	   if (ip != null) {
	      ip.setTitle(title_ + ": " + statusLine);
	      ip.updateAndDraw();
	   }
	}
	
//!!!	// public
/*
	public void newImage(Object img) {

		if (getImageWindowByteLength() != imageByteLenth(img)) {
			throw (new RuntimeException("Image bytelenth does not much"));
		}
		getImagePlus().getProcessor().setPixels(img);
		getImagePlus().updateAndDraw();
		// Graphics
		// getCanvas().paint(getCanvas().getGraphics());
		updateHistogram();
		// update coordinate and pixel info in imageJ by simulating mouse
		// move
		Point pt = getCanvas().getCursorLoc();
		getImagePlus().mouseMoved(pt.x, pt.y);
	}
*/	
	// Set ImageJ pixel calibration
	public void setIJCal() {
		double pixSizeUm = core_.getPixelSizeUm();
		Calibration cal = new Calibration();
		if (pixSizeUm > 0) {
			cal.setUnit("um");
			cal.pixelWidth = pixSizeUm;
			cal.pixelHeight = pixSizeUm;
		}
		getImagePlus().setCalibration(cal);
	}

	public long getRawHistogramSize() {
		long ret = 0;
      ImagePlus ip = getImagePlus();
      if (ip != null) {
         ImageProcessor pp = ip.getProcessor();
         if (pp != null)  {
            int rawHistogram[] = pp.getHistogram();
            if (rawHistogram != null) {
               ret = rawHistogram.length;
            }
         }
		}
		return ret;
	}
	
    public void windowClosing(WindowEvent e) {
    	gui_.enableLiveMode(false);
    	super.windowClosing(e);
    }

    public void setSubTitle(String title) {
        title_ = title;
        ImagePlus imgp = getImagePlus();
        if (imgp != null)
            imgp.setTitle(title_);
    }

     public void tearFreeUpdate() {
       Graphics g = this.getCanvas().getGraphics();
       //core_.WaitForScreenRefresh();
       this.getCanvas().paint(g);
    }
}