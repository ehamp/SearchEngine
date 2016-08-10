package CLIapplication.javacs;


import java.awt.Desktop;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import redis.clients.jedis.Jedis;
import static CLIapplication.javacs.WikiSearch.search;
import CLIapplication.javacs.JedisIndex;
/**
 *
 * @author ajenejohnson
 */


public class SearchEngineCLI extends Application {
    
    private static Scene scene;
    private StackPane root;
    private TextField commandTextField;
    private OptionParser parser;
    private VBox box;
    private Label label;
    private List<Hyperlink> urlList = new ArrayList<>();
    private List<String> strUrlList = new ArrayList<>();
    private ScrollPane sp;
    public static Map<String, String> descMap = new HashMap<>();
    @Override
    public void start(Stage primaryStage) {
        
        setUpParser();
        
        BackgroundFill bgf = new BackgroundFill(Paint.valueOf("black"), CornerRadii.EMPTY, Insets.EMPTY);
        Background bg = new Background(bgf);
        box = new VBox();
        //box.setAlignment(Pos.TOP_CENTER);
        box.setBackground(bg);
        box.setMinHeight(1000);
        
        BackgroundFill bgf1 = new BackgroundFill(Paint.valueOf("#272822"), CornerRadii.EMPTY, Insets.EMPTY );
        Background bg1 = new Background(bgf1);
        commandTextField = new TextField();
        commandTextField.setBackground(bg1);
        commandTextField.setStyle("-fx-text-fill: #66D9EF;");
        setUpCommands();
        
        sp = new ScrollPane();
        sp.setBackground(bg);
        sp.setMaxHeight(725);
        sp.setFitToWidth(true);
        sp.setContent(box);
        
        root = new StackPane();
        root.getChildren().add(sp);
        root.getChildren().add(commandTextField);
        root.setAlignment(commandTextField, Pos.BOTTOM_CENTER);
        root.setAlignment(sp, Pos.TOP_CENTER);
        
        scene = new Scene(root, 890, 750);
        
        primaryStage.setTitle("Search Engine");
        primaryStage.setScene(scene);
        primaryStage.show();
        
    }
    
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
        
    }
    
    public void setUpParser(){
        parser = new OptionParser();
        parser.accepts("and", "intersection of terms");
        parser.accepts("or", "union of terms");
        parser.accepts("minus", "difference of terms");
        parser.accepts("clear", "clear console");
        parser.accepts("help", "show help").forHelp();
    }
    
    public void setUpCommands(){
        
        commandTextField.setOnAction((ActionEvent event) -> {
            
            box.getChildren().clear();
            urlList.clear();
            String command = commandTextField.getText();
            Scanner scnnr = new Scanner(command);
            List<String> enteredText = new ArrayList<>();
            while(scnnr.hasNext()){
                enteredText.add(scnnr.next());
            }
            int correctindex = 0;
            try{
                correctindex = 1;
                enteredText.get(correctindex);
            }catch(IndexOutOfBoundsException e){
                correctindex = 0;
            }
            
            
            try{
                OptionSet options = parser.parse("-"+enteredText.get(correctindex));
                Jedis jedis = JedisMaker.make();
                JedisIndex index = new JedisIndex(jedis);
                String term1 = "";
                String term2 = "";
                //To test description
               // WikiCrawler wc = new WikiCrawler("https://en.wikipedia.org/wiki/Java_(programming_language)",index);
               // wc.doCrawl();
                try{
                    term1 = enteredText.get(0);
                    term2 = enteredText.get(2);
                } catch(IndexOutOfBoundsException e){
                    Label lbl = new Label();
                    lbl.setText("Invalid input");
                    lbl.setStyle("-fx-text-fill: white;");
                    box.getChildren().add(lbl);
                }
                
                WikiSearch search1 = null;
                WikiSearch search2 = null;
                
                if(!term1.equals("") && !term2.equals("")){
                    search1 = search(term1, index);
                    search2 = search(term2, index);
                }
                 
                if(options.has("and")){
                    
                    WikiSearch intersection = search1.and(search2);
                    Map<String, String> searchAndDescriptions = intersection.getDescriptionsFromRedis(intersection);
                    
                    label = new Label("And Results: " + term1 + " " + term2);
                    label.setStyle("-fx-text-fill: #F92672;");
                    box.getChildren().add(label);
                    for(Entry<String, Double> entry: intersection.getList()){
                        
                        Hyperlink link = new Hyperlink();
                        link.setText(entry.getKey());
                        link.setTextFill(Paint.valueOf("#66D9EF"));
                        link.setOnAction((ActionEvent e) -> {
                            openWebpage(entry.getKey());
                        });
                        
                        urlList.add(link);
                        strUrlList.add(entry.getKey());
                    }
                    
                    if(urlList.isEmpty() || !index.termSet().contains(term1) || !index.termSet().contains(term2)){
                        Label lbl = new Label();
                        lbl.setText("No links were found");
                        lbl.setStyle("-fx-text-fill: white;");
                        box.getChildren().add(lbl);
                    }else{
                        Collections.reverse(urlList);
                        Collections.reverse(strUrlList);
                        for(int i = 0; i < urlList.size(); i++){
                            box.getChildren().add(urlList.get(i));
                            Label lbl = new Label();
                            lbl.setText(searchAndDescriptions.get(strUrlList.get(i)));
                            lbl.setStyle("-fx-text-fill: white;");
                            box.getChildren().add(lbl);
                        }
                        
                    }
                   
                    //System.out.println("Contents of map is " +WikiCrawler.descriptionMap.get("https://en.wikipedia.org/wiki/Java_virtual_machine"));
                }else if(options.has("or")){

                    WikiSearch union = search1.or(search2);
                    Map<String, String> searchUnionDescriptions = union.getDescriptionsFromRedis(union);
                    label = new Label("Or Results: " + term1 + " " + term2);
                    label.setStyle("-fx-text-fill: #F92672;");
                    box.getChildren().add(label);
                    
                    for(Entry<String, Double> entry: union.getList()){
                        
                        Hyperlink link = new Hyperlink();
                        link.setText(entry.getKey());
                        link.setTextFill(Paint.valueOf("#66D9EF"));
                        link.setOnAction((ActionEvent e) -> {
                            openWebpage(entry.getKey());
                        });
                        
                        urlList.add(link);
                        strUrlList.add(entry.getKey());
                    }
                    
                    if(urlList.isEmpty() || !index.termSet().contains(term1) || !index.termSet().contains(term2)){
                        Label lbl = new Label();
                        lbl.setText("No links were found");
                        lbl.setStyle("-fx-text-fill: white;");
                        box.getChildren().add(lbl);
                    }else{
                        
                        Collections.reverse(urlList);
                        Collections.reverse(strUrlList);
                        for(int i = 0; i < urlList.size(); i++){
                            box.getChildren().add(urlList.get(i));
                            Label lbl = new Label();
                            lbl.setText(searchUnionDescriptions.get(strUrlList.get(i)));
                            lbl.setStyle("-fx-text-fill: white;");
                            box.getChildren().add(lbl);
                        }
                        
                    }

                }else if(options.has("minus")){

                    WikiSearch difference = search1.minus(search2);
                    Map<String, String> searchMinusDescriptions = difference.getDescriptionsFromRedis(difference);
                    label = new Label("Minus Results: " + term1 + " " + term2);
                    label.setStyle("-fx-text-fill: #F92672;");
                    box.getChildren().add(label);
                    
                    for(Entry<String, Double> entry: difference.getList()){
                        
                        Hyperlink link = new Hyperlink();
                        link.setText(entry.getKey());
                        link.setTextFill(Paint.valueOf("#66D9EF"));
                        link.setOnAction((ActionEvent e) -> {
                            openWebpage(entry.getKey());
                        });
                        
                        urlList.add(link);
                        strUrlList.add(entry.getKey());
                    }
                    
                    if(urlList.isEmpty() || !index.termSet().contains(term1) || !index.termSet().contains(term2)){
                        Label lbl = new Label();
                        lbl.setText("No links were found");
                        lbl.setStyle("-fx-text-fill: white;");
                        box.getChildren().add(lbl);
                    }else{
                        
                        Collections.reverse(urlList);
                        Collections.reverse(strUrlList);
                        for(int i = 0; i < urlList.size(); i++){
                            box.getChildren().add(urlList.get(i));
                            Label lbl = new Label();
                            lbl.setText(searchMinusDescriptions.get(strUrlList.get(i)));
                            lbl.setStyle("-fx-text-fill: white;");
                            box.getChildren().add(lbl);
                        }
                        
                    }

                }else if (options.has("clear")){
                
                    box.getChildren().clear();
                    commandTextField.clear();
                
                }else if (options.has("help")){
                    
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    PrintStream old = System.out;
                    System.setOut(ps);

                    parser.printHelpOn(System.out);

                    System.out.flush();
                    System.setOut(old);
                    label = new Label(baos.toString());
                    label.setStyle("-fx-text-fill: white;");
                    box.getChildren().add(label);
                  }
                
                commandTextField.clear();
                sp.setContent(box);
            }catch(Exception e){
                commandTextField.clear();
                Label lbl = new Label("There was an error, please try again");
                lbl.setStyle("-fx-text-fill: white;");
                box.getChildren().add(lbl);
                e.printStackTrace();
            }
        });
    }
    
    public static void openWebpage(String urlString) {
        try {
            Desktop.getDesktop().browse(new URL(urlString).toURI());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}

