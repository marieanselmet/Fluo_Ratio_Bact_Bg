import Fluo_Ratio_Bact_Bg.Tools;
import ij.*;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import loci.plugins.BF;
import loci.plugins.util.ImageProcessorReader;
import loci.plugins.in.ImporterOptions;
import mcib3d.geom2.Objects3DIntPopulation;
import org.apache.commons.io.FilenameUtils;
import org.scijava.util.ArrayUtils;


public class Fluo_Ratio_Bact_Bg implements PlugIn {
    
    Tools tools = new Tools();
    private String imageDir = "";
    public String outDirResults = "";
    public BufferedWriter fluoResults;
    public BufferedWriter shapeResults;
   
    
    public void run(String arg) {
        try {
            if (!tools.checkInstalledModules()) {
                return;
            }
            
            imageDir = IJ.getDirectory("Choose directory containing image files");
            if (imageDir == null) {
                IJ.showMessage("Error", "No files directory");
                return;
            }
            
            // Find images with extension
            String file_ext = tools.findImageType(new File(imageDir));
            ArrayList<String> imageFiles = tools.findImages(imageDir, file_ext);
            if (imageFiles.isEmpty()) {
                IJ.showMessage("Error", "No images found");
                return;
            }
            
            // Create results output folder
            outDirResults = imageDir + File.separator + "Results" + File.separator;
            File outDir = new File(outDirResults);
            if (!Files.exists(Paths.get(outDirResults))) {
                outDir.mkdir();
            }
            // Write header in results file
            String header = "Image name\tFrame number\t Bacterium ID\tBacterium surface (µm2)\tBacterium length (µm)\tBacterium intensity"
                    +"\tBackground intensity \tBacterium intensity / Background intensity\n";
            FileWriter fwFluoResults = new FileWriter(outDirResults + "fluo_results.xls", false);
            fluoResults = new BufferedWriter(fwFluoResults);
            fluoResults.write(header);
            fluoResults.flush();
                    
            header = "Image name\tFrame number\tBact ID\t Bacterium area\tBacterium feret\t"
                    + "Bacterium feret min\tBacterium cicularity\t"
                    + "Bacterium aspect ratio\t"+ "Bacterium roundness\n";
            
            FileWriter fwShapeResults = new FileWriter(outDirResults + "shape_results.xls", false);
            shapeResults = new BufferedWriter(fwShapeResults);
            shapeResults.write(header);
            shapeResults.flush();
            
            // Create OME-XML metadata store of the latest schema version
            ServiceFactory factory;
            factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            IMetadata meta = service.createOMEXMLMetadata();
            ImageProcessorReader reader = new ImageProcessorReader();
            reader.setMetadataStore(meta);
            reader.setId(imageFiles.get(0));
            
            // Find image calibration
            tools.findImageCalib(meta);
            
            // Find channels name
            String[] channels = tools.findChannels(imageFiles.get(0), meta, reader);
            
            // Dialog box
            String[] chs = tools.dialog(channels);
            if (chs == null) {
                IJ.showMessage("Error", "Plugin canceled");
                return;
            } 

            for (String f : imageFiles) {
                reader.setId(f);
                String rootName = FilenameUtils.getBaseName(f);
                System.out.println("-- ANALYZING IMAGE " + rootName + " --");
                
                ImporterOptions options = new ImporterOptions();
                options.setId(f);
                options.setQuiet(true);
                options.setColorMode(ImporterOptions.COLOR_MODE_GRAYSCALE);
                options.setSplitChannels(true);
                
                // Open phase contrast channel
                int indexCh = ArrayUtils.indexOf(channels, chs[0]);
                ImagePlus imgPhase = BF.openImagePlus(options)[indexCh];
                
                // Open fluo channel
                indexCh = ArrayUtils.indexOf(channels, chs[1]);
                ImagePlus imgFluo = BF.openImagePlus(options)[indexCh];
                
                for(int t=1; t < imgPhase.getNFrames() + 1; t++) {
                    
                    // Open frame t for channel 0
                    ImagePlus tPhase = new Duplicator().run​(imgPhase, 1, 1, 1, 1, t, t);
                    // Detect bacteria with Omnipose
                    System.out.println("- Detecting bacteria on phase contrast channel -");
                    Objects3DIntPopulation tbactPop = tools.omniposeDetection(tPhase);
                    System.out.println(tbactPop.getNbObjects() + " bacteria found on frame " + t);
                    
                    // Open frame t for channel 1
                    ImagePlus tFluo = new Duplicator().run​(imgFluo, 1, 1, 1, 1, t, t);
                    double tBackground = tools.findRoiBackgroundAuto(tFluo, 100, "median"); // rolling ball radius of 100 pixels
                        
                    // Do measurements and save results
                    System.out.println("- Saving results -");
                    tools.saveResults(tbactPop, tPhase, tFluo, tBackground, rootName, fluoResults, shapeResults, t);
                
                    // Save images
                    tools.drawResults(tPhase, tFluo, tbactPop, outDirResults+rootName, outDirResults, t);

                }
 
                tools.flush_close(imgPhase);
                tools.flush_close(imgFluo); 
            }

            System.out.println("--Done !--");
            
        }   catch (IOException | FormatException | DependencyException | ServiceException ex) {
            Logger.getLogger(Fluo_Ratio_Bact_Bg.class.getName()).log(Level.SEVERE, null, ex);
        }  
    }
}    
