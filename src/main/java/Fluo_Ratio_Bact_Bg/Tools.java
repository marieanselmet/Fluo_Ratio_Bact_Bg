package Fluo_Ratio_Bact_Bg;
import Fluo_Ratio_Bact_Bg_Tools.CellposeTaskSettings;
import Fluo_Ratio_Bact_Bg_Tools.CellposeSegmentImgPlusAdvanced;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.measure.Calibration;
import ij.plugin.Duplicator;
import ij.gui.Roi;
import fiji.util.gui.GenericDialogPlus;
import ij.measure.ResultsTable;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.ImageProcessor;
import java.awt.Color;
import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.plugins.util.ImageProcessorReader;
import mcib3d.geom2.Object3DInt;
import mcib3d.geom2.Objects3DIntPopulation;
import mcib3d.geom2.Objects3DIntPopulationComputation;
import mcib3d.geom2.VoxelInt;
import mcib3d.geom2.measurements.MeasureFeret;
import mcib3d.geom2.measurements.MeasureIntensity;
import mcib3d.geom2.measurements.MeasureVolume;
import mcib3d.image3d.ImageHandler;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.NullPrintStream;

public class Tools {

    public Calibration cal = new Calibration();
    private double pixelSurf = 0;
    String[] channelsName = {"Phase: ", "Fluo: "};
    
     // Omnipose settings
    private String omniposeEnvDirPath = "/opt/AppData/Local/miniconda3/envs/omnipose";
    private String omniposeModelsPath = System.getProperty("user.home")+"/.cellpose/models/";
    private String omniposeModel = "bact_phase_omnitorch_0";
     
    private int omniposeDiameter = 30; //50
    private int omniposeMaskThreshold = 0;
    private double omniposeFlowThreshold = 0.4;
    private boolean useGpu = true;
     
    private double minBactSurface = 1;
    private double maxBactSurface = 10;
    
    
    public boolean checkInstalledModules() {
        ClassLoader loader = IJ.getClassLoader();
        try {
            loader.loadClass("mcib3d.geom.Object3D");
        } catch (ClassNotFoundException e) {
            IJ.showMessage("Error", "3D ImageJ Suite not installed");
            return false;
        }
        return true;
    }
    
    
    public void flush_close(ImagePlus img) {
        img.flush();
        img.close();
    }
    

    public String findImageType(File imagesFolder) {
        String ext = "";
        String[] files = imagesFolder.list();
        for (String name : files) {
            String fileExt = FilenameUtils.getExtension(name);
            switch (fileExt) {
                case "nd" :
                   ext = fileExt;
                   break;
                case "nd2" :
                   ext = fileExt;
                   break;
                case "czi" :
                   ext = fileExt;
                   break;
                case "lif"  :
                    ext = fileExt;
                    break;
                case "ics" :
                    ext = fileExt;
                    break;
                case "ics2" :
                    ext = fileExt;
                    break;
                case "lsm" :
                    ext = fileExt;
                    break;
                case "tif" :
                    ext = fileExt;
                    break;
                case "tiff" :
                    ext = fileExt;
                    break;
            }
        }
        return(ext);
    }
     

    public ArrayList<String> findImages(String imagesFolder, String imageExt) {
        File inDir = new File(imagesFolder);
        String[] files = inDir.list();
        if (files == null) {
            System.out.println("No image found in " + imagesFolder);
            return null;
        }
        ArrayList<String> images = new ArrayList();
        for (String f : files) {
            String fileExt = FilenameUtils.getExtension(f);
            if (fileExt.equals(imageExt) && !f.startsWith("."))
                images.add(imagesFolder + File.separator + f);
        }
        Collections.sort(images);
        return(images);
    }

    
    public void findImageCalib(IMetadata meta) {
        cal.pixelWidth = meta.getPixelsPhysicalSizeX(0).value().doubleValue();
        cal.pixelHeight = cal.pixelWidth; // Assuming isotropy (this is the case in all our images)
        cal.setUnit("microns");
        System.out.println("XY calibration = " + cal.pixelWidth);
    }
    
    
    public String[] findChannels (String imageName, IMetadata meta, ImageProcessorReader reader) throws DependencyException, ServiceException, FormatException, IOException {
        int chs = reader.getSizeC();
        String[] channels = new String[chs];
        String imageExt =  FilenameUtils.getExtension(imageName);
        switch (imageExt) {
            case "nd" :
                for (int n = 0; n < chs; n++)
                {
                    if (meta.getChannelID(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelName(0, n);
                }
                break;
            case "nd2" :
                for (int n = 0; n < chs; n++) 
                {
                    if (meta.getChannelID(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelName(0, n);
                }
                break;
            case "lif" :
                for (int n = 0; n < chs; n++) 
                    if (meta.getChannelID(0, n) == null || meta.getChannelName(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelName(0, n);
                break;
            case "czi" :
                for (int n = 0; n < chs; n++) 
                    if (meta.getChannelID(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelFluor(0, n);
                break;
            case "ics" :
                for (int n = 0; n < chs; n++) 
                    if (meta.getChannelID(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelExcitationWavelength(0, n).value().toString();
                break;
            case "ics2" :
                for (int n = 0; n < chs; n++) 
                    if (meta.getChannelID(0, n) == null)
                        channels[n] = Integer.toString(n);
                    else 
                        channels[n] = meta.getChannelExcitationWavelength(0, n).value().toString();
                break;   
            default :
                for (int n = 0; n < chs; n++)
                    channels[n] = Integer.toString(n);
        }
        return(channels);         
    }
    

    public String[] dialog(String[] channels) {
        GenericDialogPlus gd = new GenericDialogPlus("Parameters");
        gd.setInsets​(0, 160, 0);
        
        gd.addMessage("Channels", Font.getFont("Monospace"), Color.blue);
        int index = 0;
        for (String ch : channelsName) {
            gd.addChoice(ch, channels, channels[index]);
            index++;
        }
        
        gd.addMessage("Bacteria detection", Font.getFont("Monospace"), Color.blue);
        if (IJ.isWindows()) {
            omniposeEnvDirPath = System.getProperty("user.home")+"\\AppData\\Local\\miniconda3\\envs\\omnipose";
            omniposeModelsPath = System.getProperty("user.home")+"\\.cellpose\\models\\";
        }
        gd.addDirectoryField("Omnipose environment directory: ", omniposeEnvDirPath);
        gd.addDirectoryField("Omnipose models path: ", omniposeModelsPath); 
        gd.addMessage("Object size threshold ", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("Min bacterium surface (µm2): ", minBactSurface);
        gd.addNumericField("Max bacterium surface (µm2): ", maxBactSurface);
        gd.addMessage("Image calibration", Font.getFont("Monospace"), Color.blue);
        gd.addNumericField("XY calibration (µm):", cal.pixelWidth);
        gd.showDialog();
        
        String[] ch = new String[channelsName.length];
        for (int i = 0; i < channelsName.length; i++)
            ch[i] = gd.getNextChoice();
        if(gd.wasCanceled())
           ch = null;

        omniposeEnvDirPath = gd.getNextString();
        omniposeModelsPath = gd.getNextString();
        minBactSurface = (float) gd.getNextNumber();
        maxBactSurface = (float) gd.getNextNumber();        
        cal.pixelWidth = cal.pixelHeight = gd.getNextNumber();
        cal.pixelDepth = 1;
        pixelSurf = cal.pixelWidth*cal.pixelWidth;

        return(ch);
    }
    
   
    public Objects3DIntPopulation omniposeDetection(ImagePlus imgBact){
        // Resize to be in a Omnipose-friendly scale
        ImagePlus imgIn = null;
        boolean resize = false;
        if (imgBact.getWidth() < 500) {
            float factor = 2f;
            imgIn = imgBact.resize((int)(imgBact.getWidth()*factor), (int)(imgBact.getHeight()*factor), 1, "bicubic");
            resize = true;
        } else {
            imgIn = new Duplicator().run(imgBact);
        }
        imgIn.setCalibration(cal);
        
        // Set Omnipose settings
        CellposeTaskSettings settings = new CellposeTaskSettings(omniposeModelsPath+omniposeModel, 1, omniposeDiameter, omniposeEnvDirPath);
        settings.setVersion("0.7");
        settings.setCluster(true);
        settings.setOmni(true);
        settings.useMxNet(false);
        settings.setCellProbTh(omniposeMaskThreshold);
        settings.setFlowTh(omniposeFlowThreshold);
        settings.useGpu(useGpu);
        
        // Run Omnipose
        CellposeSegmentImgPlusAdvanced cellpose = new CellposeSegmentImgPlusAdvanced(settings, imgIn);
        PrintStream console = System.out;
        System.setOut(new NullPrintStream());
        ImagePlus imgOut = cellpose.run();
        System.setOut(console);
        imgOut.setCalibration(cal);
        
        Objects3DIntPopulation pop = new Objects3DIntPopulation(ImageHandler.wrap(imgOut));
        pop = new Objects3DIntPopulationComputation(pop).getExcludeBorders(ImageHandler.wrap(imgOut), false);
        pop = new Objects3DIntPopulationComputation(pop).getFilterSize(minBactSurface/pixelSurf, maxBactSurface/pixelSurf);
        pop.resetLabels();
        
        // Close images
        flush_close(imgIn);
        flush_close(imgOut);
        
        return(pop);
    }
   
  
    public double findBackground(ImagePlus img, Roi roi, String method) {
      ImagePlus imgProj =  new Duplicator().run(img);
      ImageProcessor imp = img.getProcessor();
      imp.setRoi(roi);

      double bg = (method.equals("median")) ? imp.getStatistics().median : imp.getStatistics().mean;
      flush_close(imgProj);
      return(bg);
    }
    

    public double findRoiBackgroundAuto(ImagePlus img, int roiBgSize, String method) {
        // Scroll image and measure median intensity in roi
        // Take roi with the lowest intensity as the output background estimate
        ArrayList<RoiBg> intBgFound = new ArrayList<>();
        for (int x = 0; x < img.getWidth() - roiBgSize; x += roiBgSize) {
            for (int y = 0; y < img.getHeight() - roiBgSize; y += roiBgSize) {
                Roi roi = new Roi(x, y, roiBgSize, roiBgSize);
                double bg = findBackground(img, roi, method);
                intBgFound.add(new RoiBg(roi, bg));
            }
        }
        img.deleteRoi();
        
        // Sort RoiBg on bg value ascending order
        intBgFound.sort(Comparator.comparing(RoiBg::getBgInt));
        // Return min value
        RoiBg roiBg = intBgFound.get(0);
        return roiBg.getBgInt();
    }
    
    
   
    public void saveResults(Objects3DIntPopulation bactPop, ImagePlus phaseImg, ImagePlus fluoImg, double background, String imgName, BufferedWriter fluoFile, BufferedWriter shapeFile, int frameNumber) throws IOException {
        for (Object3DInt bact : bactPop.getObjects3DInt()) {
            float bactLabel = bact.getLabel();
            double bactSurf = new MeasureVolume(bact).getValueMeasurement(MeasureVolume.VOLUME_UNIT);
            VoxelInt feret1Unit = new MeasureFeret(bact).getFeret1Unit();
            VoxelInt feret2Unit = new MeasureFeret(bact).getFeret2Unit();
            double bactLength = feret1Unit.distance(feret2Unit)*cal.pixelWidth;
            
            // Fluo descriptors
            double fluoIntensity = new MeasureIntensity(bact, ImageHandler.wrap(fluoImg)).getValueMeasurement(MeasureIntensity.INTENSITY_AVG);
            fluoFile.write(imgName+"\t"+frameNumber+"\t"+bactLabel+"\t"+bactSurf+"\t"+bactLength+"\t"+fluoIntensity+"\t"+background+"\t"+fluoIntensity/background+"\n");
        
            // Bacteria shape descriptors
            ImageHandler imh = ImageHandler.wrap(phaseImg).createSameDimensions();
            // Draw each object independently (and not all the population of objects once) to avoid binary thresholding of cell aggregates to results in merging several cells and biasing the shape results
            bact.drawObject(imh);
            ResultsTable resultsTable = new ResultsTable();
            ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.CLEAR_WORKSHEET, ParticleAnalyzer.SHAPE_DESCRIPTORS+ParticleAnalyzer.LIMIT
                    +ParticleAnalyzer.AREA+ParticleAnalyzer.FERET, resultsTable, 0, Double.MAX_VALUE);
            IJ.setThreshold(imh.getImagePlus(), 1, Double.MAX_VALUE);
            particleAnalyzer.analyze(imh.getImagePlus());
            double area = resultsTable.getValue("Area", 0);
            double feret = resultsTable.getValue("Feret", 0);
            double minFeret = resultsTable.getValue("MinFeret", 0);
            double cir = resultsTable.getValue("Circ.", 0);
            double ar = resultsTable.getValue("AR", 0);
            double round = resultsTable.getValue("Round", 0);
             shapeFile.write(imgName+"\t"+frameNumber+"\t"+bactLabel+"\t"+area+"\t"+feret+"\t"+minFeret+
                     "\t"+cir+"\t"+ar+"\t"+round+"\n");
        }
        fluoFile.flush();
        shapeFile.flush();
    }
    

    public void drawResults(ImagePlus imgBact, ImagePlus imgGene, Objects3DIntPopulation bactPop, String imgName, String outDir, int frameNumber) {
        ImageHandler imhBact = ImageHandler.wrap(imgBact).createSameDimensions();
        bactPop.drawInImage(imhBact);
        IJ.run(imhBact.getImagePlus(), "glasbey on dark", "");
        ImagePlus[] imgColors = {imhBact.getImagePlus(), imgBact};
        ImagePlus imgOut = new RGBStackMerge().mergeHyperstacks(imgColors, false);
        imgOut.setCalibration(cal);
        FileSaver ImgObjectsFile = new FileSaver(imgOut);
        ImgObjectsFile.saveAsTiff(imgName + "_frame" + frameNumber + "_bacteria.tif");      
        flush_close(imgOut);
    }
}
