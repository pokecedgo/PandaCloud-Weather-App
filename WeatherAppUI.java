package weatherappant;
//alter this package when I move from laptops
//Other
import org.json.simple.JSONArray;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
//WeatherAPI
import org.json.simple.JSONObject;
//Date API
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;    
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Scanner;
import java.util.TimeZone;
import javax.swing.ImageIcon;
import org.json.simple.parser.JSONParser;




public class WeatherAppUI extends javax.swing.JFrame {
    Timer timer;
    private JSONObject weatherData; 
    
    public WeatherAppUI() {
        initComponents();
        Display_Date();
        setupSearchButton();
        // Call Display_Date first, then use the value updated in Location_Display to call getLocationData
        String userCity = Location_Display.getText();
        System.out.println(getLocationData(userCity));

        //Time Display
        timer = new Timer(60000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Display_Date();
            }
        });
        timer.start();
    }
    //Search Buttons
    private void setupSearchButton() {
        SearchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeSearch();           
            }
        });
    }
    
    private void executeSearch() {
        String userInput = SearchBox.getText();
        
        if(userInput.replaceAll("\\s", "").length() <= 0) {
            return;
        }
        
        //retrieve weather data
        weatherData = WeatherAppUI.getWeatherData(userInput);
        
        System.out.println(getLocationData("Tokyo"));
         
         
        if (weatherData == null) {
            // Handle null case: maybe show an error message to the user or return from the method
           System.err.println("Error: weatherData is null!");
           return; // end the method early
        }
             
        //display title location
        String location_head = userInput.substring(1, userInput.length()-1); 
        String capitalizedUserInput = userInput.substring(0, 1).toUpperCase() + userInput.substring(1);

        // Set the capitalized string to Location_Search_Display
        Location_Search_Display.setText(capitalizedUserInput);

        //update weather image
        String weatherCondition = (String) weatherData.get("weather_condition");
        
         //we could compare time + condition to do specific images
        switch(weatherCondition) {
            case "Clear Sky":
                Weather_Display.setIcon(new ImageIcon(getClass().getResource("/Assets/sun.png")));
                break;
            case "Sunny": // Added case for Sunny
                Weather_Display.setIcon(new ImageIcon(getClass().getResource("/Assets/sunny.png"))); // Ensure you have a sunny.png asset
                break;
            case "Cloudy":
                Weather_Display.setIcon(new ImageIcon(getClass().getResource("/Assets/cloudy.png")));
                break;
            case "Snow":
                Weather_Display.setIcon(new ImageIcon(getClass().getResource("/Assets/snowing.png")));
                break;
            case "Rain":
                Weather_Display.setIcon(new ImageIcon(getClass().getResource("/Assets/rain.png")));
                break;
            case "Fog": // Added case for Fog
                Weather_Display.setIcon(new ImageIcon(getClass().getResource("/Assets/windy.png"))); // Ensure you have a fog.png asset
                break;
            case "Thunderstorm": // Added case for Thunderstorm
                Weather_Display.setIcon(new ImageIcon(getClass().getResource("/Assets/storm.png"))); // Ensure you have a thunderstorm.png asset
                break;
            default: // Added default case for any other condition
                Weather_Display.setIcon(new ImageIcon(getClass().getResource("/Assets/meteorology.png"))); // Ensure you have an unobserved.png or similar asset for unknown conditions
                break;
        }

        SearchBox.setText("");
         // update temperature text
         double temperature = (double) weatherData.get("temperature");
         Temperature_Display.setText(temperature + " C");

         // update weather condition text
         WeatherCondition_Display.setText(weatherCondition);

         // update humidity text
         long humidity = (long) weatherData.get("humidity");
         Humidity_Display.setText(humidity + "%");

         // update windspeed text
          double windspeed = (double) weatherData.get("windspeed");
          WindSpeed_Display.setText(windspeed + "km");
    }
    
    public static JSONObject getWeatherData(String locationName) {
        //get location coordinates using geolocation API
        JSONArray locationData = getLocationData(locationName);
        
        
        //extract latitude and longtitude
        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");
        
        //request URL with location coordinates
        String urlString = "https://api.open-meteo.com/v1/forecast?"
                + "latitude=" + latitude + "&longitude=" + longitude 
                + "&hourly=temperature_2m,relativehumidity_2m,weathercode,windspeed_10m&timezone=America%2FNew_York";
        
        
         try{
            // call api and get response
            HttpURLConnection conn = fetchApiResponse(urlString);

            // check for response status
            // 200 - means that the connection was a success
            if(conn.getResponseCode() != 200){
                System.out.println("Error: Could not connect to API");
                return null;
            }

            // store resulting json data
            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while(scanner.hasNext()){
                // read and store into the string builder
                resultJson.append(scanner.nextLine());
            }

            // close scanner
            scanner.close();

            // close url connection
            conn.disconnect();

            // parse through our data
            JSONParser parser = new JSONParser();
            JSONObject resultJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

            // retrieve hourly data
            JSONObject hourly = (JSONObject) resultJsonObj.get("hourly");

            // we want to get the current hour's data
            // so we need to get the index of our current hour
            JSONArray time = (JSONArray) hourly.get("time");
            int index = findIndexOfCurrentTime(time);

            // get temperature
            JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
            double temperature = (double) temperatureData.get(index);

            // get weather code
            JSONArray weathercode = (JSONArray) hourly.get("weathercode");
            String weatherCondition = convertWeatherCode((long) weathercode.get(index));

            // get humidity
            JSONArray relativeHumidity = (JSONArray) hourly.get("relativehumidity_2m");
            long humidity = (long) relativeHumidity.get(index);

            // get windspeed
            JSONArray windspeedData = (JSONArray) hourly.get("windspeed_10m");
            double windspeed = (double) windspeedData.get(index);

            // build the weather json data object that we are going to access in our frontend
            JSONObject weatherData = new JSONObject();
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("windspeed", windspeed);

            return weatherData;
        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }
    
    public static JSONArray getLocationData(String locationName){
        // replace any whitespace in location name to + to adhere to API's request format
        locationName = locationName.replaceAll(" ", "+");

        // build API url with location parameter
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                locationName + "&count=10&language=en&format=json";

        try{
            // call api and get a response
            HttpURLConnection conn = fetchApiResponse(urlString);

            // check response status
            // 200 means successful connection
            if(conn.getResponseCode() != 200){
                System.out.println("Error: Could not connect to API");
                return null;
            }else{
                // store the API results
                StringBuilder resultJson = new StringBuilder();
                Scanner scanner = new Scanner(conn.getInputStream());

                // read and store the resulting json data into our string builder
                while(scanner.hasNext()){
                    resultJson.append(scanner.nextLine());
                }

                // close scanner
                scanner.close();

                // close url connection
                conn.disconnect();

                // parse the JSON string into a JSON obj
                JSONParser parser = new JSONParser();
                JSONObject resultsJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

                // get the list of location data the API gtenerated from the lcoation name
                JSONArray locationData = (JSONArray) resultsJsonObj.get("results");
                return locationData;
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        // couldn't find location
        return null;
    }
    
    private static HttpURLConnection fetchApiResponse(String urlString) {
        try{
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            //set request method to get
            conn.setRequestMethod("GET");
            
            //connect to API
            conn.connect();
            return conn;
            
        }catch(IOException e) {
            e.printStackTrace();
        }
        //no connection made
        return null;
    }
    
    private static int findIndexOfCurrentTime(JSONArray timeList) {
        String currentTime = getCurrentTime();
        
        
        //iterat ethrough the time list and see which one matches our current time
        for(int i = 0; i<timeList.size();i++) {
            String time = (String) timeList.get(i);
            
            if(time.equalsIgnoreCase(currentTime)) {
                return i;
            }
                
        }
        
        return 0;
    }
    
    public static String getCurrentTime() {
        //get current data and time
        LocalDateTime currentDateTime = LocalDateTime.now();
        //formatting date to be 2023-09-02T00:00
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00)'");
        
        //format
        String formattedDateTime = currentDateTime.format(formatter);
        
        return formattedDateTime;
        
    }
    
    private static String convertWeatherCode(long weathercode) {
        String weatherCondition = "";
        if(weathercode == 0L) {
            weatherCondition = "Clear Sky";
        } else if(weathercode >= 1L && weathercode <= 3L) {
            weatherCondition = "Sunny"; // Adjust this condition to represent sunny weather
        } else if(weathercode >= 51L && weathercode <= 67L || (weathercode >= 80L && weathercode <= 99L)) {
            weatherCondition ="Rain";
        } else if(weathercode >= 71L && weathercode <= 82L) {
            weatherCondition = "Snow";
        } else if (weathercode >= 45L && weathercode <= 48L) {
            weatherCondition = "Fog";
        } else if (weathercode >= 95 && weathercode <= 99) {
            weatherCondition = "Thunderstorm";
        } else {
            weatherCondition = "Unobserved";
        }

        return weatherCondition;
    }

            
    public static String getZone(ZonedDateTime localZonedDateTime) {
    String cityName = "Unknown City";
    
    ZonedDateTime estZonedDateTime = localZonedDateTime.withZoneSameInstant(ZoneId.of("America/New_York"));
    long hoursDifference = Duration.between(estZonedDateTime.toLocalTime(), localZonedDateTime.toLocalTime()).toHours();
    hoursDifference = hoursDifference % 24; // Handle cases crossing midnight

    switch ((int) hoursDifference) {
        case 0:
            cityName = "New York";
            break;
        case -1: case 23:
            cityName = "Chicago";
            break;
        case -2: case 22:
            cityName = "Denver";
            break;
        case -3: case 21:
            cityName = "Phoenix";
            break;
        case -4: case 20:
            cityName = "Los Angeles";
            break;
        case -5: case 19:
            cityName = "Anchorage";
            break;
        case -6: case 18:
            cityName = "Honolulu";
            break;
        case 5: case -19:
            cityName = "London";
            break;
        case 6: case -18:
            cityName = "Paris";
            break;
        case 14: case -10:
            cityName = "Tokyo";
            break;
        
    }

    return cityName;
}
    
    
    public void Display_Date() {
        // Get the user's local date and time
        LocalDateTime localNow = LocalDateTime.now();
        ZoneId localZoneId = ZoneId.systemDefault();
        ZonedDateTime localZonedDateTime = localNow.atZone(localZoneId);

        DateTimeFormatter dateDtf = DateTimeFormatter.ofPattern("MMMM d");
        DateTimeFormatter timeDtf = DateTimeFormatter.ofPattern("h:mm a");

        DayOfWeek dayOfWeek = localZonedDateTime.getDayOfWeek();
        String currentDay = dayOfWeek.toString().substring(0, 1).toUpperCase() + dayOfWeek.toString().substring(1).toLowerCase();
        String currentDate = dateDtf.format(localZonedDateTime);
        String currentTime = timeDtf.format(localZonedDateTime);


        Day_Display.setText(currentDay);
        Month_Display.setText(currentDate);
        Time_Display.setText(currentTime);



        ZonedDateTime estZonedDateTime = localZonedDateTime.withZoneSameInstant(ZoneId.of("America/New_York"));

        Location_Display.setText(getZone(estZonedDateTime));

        //display weather based on location
        /*
        JSONArray locationData = getLocationData(getZone(estZonedDateTime));
        System.out.println(locationData);
        System.out.println(getLocationData("Tokyo"));*/
    }



    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        MainFrame = new javax.swing.JPanel();
        Day_Display = new javax.swing.JLabel();
        Month_Display = new javax.swing.JLabel();
        Location_Display = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        Weather_Display = new javax.swing.JLabel();
        SearchBox = new javax.swing.JTextField();
        SearchButton = new javax.swing.JButton();
        WeatherCondition_Display = new javax.swing.JLabel();
        Display_Text = new javax.swing.JLabel();
        BottomFrame = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        WindSpeed_Display = new javax.swing.JLabel();
        Humidity_Display = new javax.swing.JLabel();
        Temperature_Display = new javax.swing.JLabel();
        icon_humid = new javax.swing.JLabel();
        icon_temp = new javax.swing.JLabel();
        Time_Display = new javax.swing.JLabel();
        Location_Search_Display = new javax.swing.JLabel();
        Display_Text1 = new javax.swing.JLabel();
        Display_Text2 = new javax.swing.JLabel();
        background = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("PandaCloud Weather ");
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        MainFrame.setBackground(new java.awt.Color(255, 255, 255));
        MainFrame.setOpaque(false);

        Day_Display.setFont(new java.awt.Font("Agency FB", 1, 65)); // NOI18N
        Day_Display.setForeground(new java.awt.Color(148, 115, 99));
        Day_Display.setText("Monday");

        Month_Display.setFont(new java.awt.Font("Agency FB", 1, 18)); // NOI18N
        Month_Display.setForeground(new java.awt.Color(255, 255, 255));
        Month_Display.setText("September 25");

        Location_Display.setFont(new java.awt.Font("Bookman Old Style", 1, 18)); // NOI18N
        Location_Display.setForeground(new java.awt.Color(255, 255, 255));
        Location_Display.setText("Highland");

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/weatherappant/location_icon.png"))); // NOI18N

        Weather_Display.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Weather_Display.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Assets/cloudy_sunny.png"))); // NOI18N

        SearchBox.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        SearchBox.setForeground(new java.awt.Color(204, 204, 204));
        SearchBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SearchBoxActionPerformed(evt);
            }
        });

        SearchButton.setBackground(new java.awt.Color(51, 51, 51));
        SearchButton.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        SearchButton.setText("Search");
        SearchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SearchButtonActionPerformed(evt);
            }
        });

        WeatherCondition_Display.setFont(new java.awt.Font("Agency FB", 1, 24)); // NOI18N
        WeatherCondition_Display.setForeground(new java.awt.Color(203, 153, 126));
        WeatherCondition_Display.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        WeatherCondition_Display.setText("Weather");

        Display_Text.setFont(new java.awt.Font("Agency FB", 1, 18)); // NOI18N
        Display_Text.setForeground(new java.awt.Color(204, 204, 204));
        Display_Text.setText("Location");

        javax.swing.GroupLayout MainFrameLayout = new javax.swing.GroupLayout(MainFrame);
        MainFrame.setLayout(MainFrameLayout);
        MainFrameLayout.setHorizontalGroup(
            MainFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MainFrameLayout.createSequentialGroup()
                .addGroup(MainFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(MainFrameLayout.createSequentialGroup()
                        .addGap(81, 81, 81)
                        .addGroup(MainFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(Day_Display, javax.swing.GroupLayout.PREFERRED_SIZE, 235, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(Month_Display, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, MainFrameLayout.createSequentialGroup()
                        .addGap(56, 56, 56)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(Location_Display, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Weather_Display, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(MainFrameLayout.createSequentialGroup()
                        .addGap(91, 91, 91)
                        .addComponent(SearchBox, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(MainFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(MainFrameLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(SearchButton))
                            .addGroup(MainFrameLayout.createSequentialGroup()
                                .addGap(2, 2, 2)
                                .addComponent(WeatherCondition_Display, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(MainFrameLayout.createSequentialGroup()
                        .addGap(105, 105, 105)
                        .addComponent(Display_Text)))
                .addContainerGap(67, Short.MAX_VALUE))
        );
        MainFrameLayout.setVerticalGroup(
            MainFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(MainFrameLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(Day_Display, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(MainFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(MainFrameLayout.createSequentialGroup()
                        .addComponent(Month_Display)
                        .addGap(29, 29, 29)
                        .addGroup(MainFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(MainFrameLayout.createSequentialGroup()
                                .addComponent(Display_Text)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(Location_Display))
                            .addComponent(jLabel3)))
                    .addComponent(Weather_Display, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(9, 9, 9)
                .addComponent(WeatherCondition_Display)
                .addGap(36, 36, 36)
                .addGroup(MainFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(SearchBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(SearchButton))
                .addGap(89, 89, 89))
        );

        Weather_Display.getAccessibleContext().setAccessibleName("Weather_Display");

        getContentPane().add(MainFrame, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 400, 340));

        BottomFrame.setOpaque(false);

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(97, 76, 68));
        jLabel2.setText("PandaCloud");

        WindSpeed_Display.setFont(new java.awt.Font("Agency FB", 1, 18)); // NOI18N
        WindSpeed_Display.setForeground(new java.awt.Color(221, 190, 169));
        WindSpeed_Display.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        WindSpeed_Display.setText("..");
        WindSpeed_Display.setToolTipText("");
        WindSpeed_Display.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        WindSpeed_Display.setVerticalTextPosition(javax.swing.SwingConstants.TOP);

        Humidity_Display.setFont(new java.awt.Font("Agency FB", 1, 18)); // NOI18N
        Humidity_Display.setForeground(new java.awt.Color(221, 190, 169));
        Humidity_Display.setText("..");

        Temperature_Display.setFont(new java.awt.Font("Agency FB", 1, 18)); // NOI18N
        Temperature_Display.setForeground(new java.awt.Color(221, 190, 169));
        Temperature_Display.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        Temperature_Display.setText("F* ");
        Temperature_Display.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        icon_humid.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Assets/meteorology.png"))); // NOI18N

        icon_temp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Assets/temperature.png"))); // NOI18N

        Time_Display.setFont(new java.awt.Font("Agency FB", 1, 18)); // NOI18N
        Time_Display.setForeground(new java.awt.Color(204, 204, 204));
        Time_Display.setText("1:28 PM");

        Location_Search_Display.setFont(new java.awt.Font("Agency FB", 1, 48)); // NOI18N
        Location_Search_Display.setForeground(new java.awt.Color(204, 204, 204));
        Location_Search_Display.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        Display_Text1.setFont(new java.awt.Font("Agency FB", 1, 18)); // NOI18N
        Display_Text1.setForeground(new java.awt.Color(204, 204, 204));
        Display_Text1.setText("Humidity");

        Display_Text2.setFont(new java.awt.Font("Agency FB", 1, 18)); // NOI18N
        Display_Text2.setForeground(new java.awt.Color(204, 204, 204));
        Display_Text2.setText("Windspeed");

        javax.swing.GroupLayout BottomFrameLayout = new javax.swing.GroupLayout(BottomFrame);
        BottomFrame.setLayout(BottomFrameLayout);
        BottomFrameLayout.setHorizontalGroup(
            BottomFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(BottomFrameLayout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addComponent(icon_humid, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(BottomFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(BottomFrameLayout.createSequentialGroup()
                        .addGap(187, 187, 187)
                        .addComponent(icon_temp))
                    .addGroup(BottomFrameLayout.createSequentialGroup()
                        .addGroup(BottomFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(Display_Text1)
                            .addComponent(Humidity_Display, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(26, 26, 26)
                        .addGroup(BottomFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(Display_Text2)
                            .addComponent(WindSpeed_Display, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(18, 18, 18)
                .addGroup(BottomFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(BottomFrameLayout.createSequentialGroup()
                        .addComponent(Time_Display)
                        .addGap(0, 14, Short.MAX_VALUE))
                    .addComponent(Temperature_Display, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, BottomFrameLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(BottomFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, BottomFrameLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, BottomFrameLayout.createSequentialGroup()
                        .addComponent(Location_Search_Display, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(22, 22, 22))))
        );
        BottomFrameLayout.setVerticalGroup(
            BottomFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, BottomFrameLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Location_Search_Display, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(BottomFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(BottomFrameLayout.createSequentialGroup()
                        .addGroup(BottomFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(BottomFrameLayout.createSequentialGroup()
                                .addGroup(BottomFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(Display_Text1)
                                    .addComponent(Display_Text2))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(BottomFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(Humidity_Display)
                                    .addComponent(WindSpeed_Display, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(icon_humid, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(BottomFrameLayout.createSequentialGroup()
                        .addGroup(BottomFrameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(Temperature_Display, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(icon_temp, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(Time_Display)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2)
                        .addGap(42, 42, 42))))
        );

        getContentPane().add(BottomFrame, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 340, 400, 210));

        background.setForeground(new java.awt.Color(153, 255, 153));
        background.setIcon(new javax.swing.ImageIcon("C:\\Users\\pokec\\OneDrive\\Documents\\NetBeansProjects\\WeatherAppAnt\\src\\weatherappant\\weather_background.png")); // NOI18N
        getContentPane().add(background, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 510, 550));

        setSize(new java.awt.Dimension(410, 555));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void SearchBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SearchBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_SearchBoxActionPerformed

    private void SearchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SearchButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_SearchButtonActionPerformed


    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(WeatherAppUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(WeatherAppUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(WeatherAppUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(WeatherAppUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new WeatherAppUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel BottomFrame;
    private javax.swing.JLabel Day_Display;
    private javax.swing.JLabel Display_Text;
    private javax.swing.JLabel Display_Text1;
    private javax.swing.JLabel Display_Text2;
    private javax.swing.JLabel Humidity_Display;
    private javax.swing.JLabel Location_Display;
    private javax.swing.JLabel Location_Search_Display;
    private javax.swing.JPanel MainFrame;
    private javax.swing.JLabel Month_Display;
    private javax.swing.JTextField SearchBox;
    private javax.swing.JButton SearchButton;
    private javax.swing.JLabel Temperature_Display;
    private javax.swing.JLabel Time_Display;
    private javax.swing.JLabel WeatherCondition_Display;
    private javax.swing.JLabel Weather_Display;
    private javax.swing.JLabel WindSpeed_Display;
    private javax.swing.JLabel background;
    private javax.swing.JLabel icon_humid;
    private javax.swing.JLabel icon_temp;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    // End of variables declaration//GEN-END:variables
}
