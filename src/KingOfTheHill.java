import lejos.robotics.subsumption.*;
import lejos.nxt.*;
import java.io.*;
/**
 * Kukkulankuningas -robotti
 *
 * Tarvitaan robotti, joka kulkee kahdella erikseen motoroidulla renkaalla.
 * Moottorit on kytketty portteihin A ja C. Robotti havainnoi ympäristöään
 * kosketussensorilla(portissa 4) ja ultraäänisensorilla(portissa 1).
 *
 * @author Jasu Viding
 */
public class KingOfTheHill {
    /**
     * Pääohjelman main metodi. Suorittaa ensimmäiseksi metodin lueLuku.
     * Tämän jälkeen alustaa behaviorit behaviorListiin,
     * kirjoittaa näytölle "King of the hill!" sekä kohdattujen
     * vaarallisten tilanteiden määrän ja jää odottamaan
     * napinpainallusta. Kun nappia painetaan, siirtyy looppaamaan
     * arbitrator.start() komentoa, jonka avulla robotti pysyy käynnissä.
     * @param args
     */
    public static void main(String[] args) {
	int luku = lueLuku(new File("data.dat"));
        Motor.A.setSpeed(250);
        Motor.C.setSpeed(250);
        Behavior b1 = new DriveForward();
        Behavior b2 = new DetectObject();
        Behavior b3 = new DetectEdge();
        Behavior[] behaviorList = {b1, b2, b3};
        Arbitrator arbitrator = new Arbitrator(behaviorList);
        LCD.drawString("King of the hill!",0,5);
        LCD.drawString("Vaarat: "+luku, 0, 1);
        Button.waitForPress();
        while(true)
            arbitrator.start();
    }
    /**
     * Jos tiedosto data.dat on olemassa jo, niin hakee siellä olevan luvun
     * ja antaa sen return arvonaan. Jos tiedostoa data.dat ei vielä ole,
     * niin returnaa arvon 0 ja luo kyseisen tiedoston.
     * @param data
     * @return luku
     */
    private static int lueLuku(File data){
        int luku=0;
        if(data.exists()){
            try{
        	InputStream is = new FileInputStream(data);
		DataInputStream din = new DataInputStream(is);
		if(is.available()>3)
                    luku=din.readInt();
                din.close();
            }
            catch(IOException ioe){
                LCD.drawString("read exception", 0, 0);
            }
        }
        return luku;
    }
}
/**
 * DetectEdge
 *
 * Nimensä mukaan havaitsee robotin kulkiessa mahdolliset vastaantulevat reunat
 * ja ohjaa robotin pois vaaratilanteesta; ensin peruuttaen pois, ja sitten
 * kääntyen kohti uutta suuntaa.
 *
 * @author Jasu Viding
 */
class DetectEdge implements Behavior {
    /**
     * Alustaa kosketussensorin luokan käyttöön.
     */
    public DetectEdge() {
        touch = new TouchSensor(SensorPort.S4);
    }
    /**
     * Metodi ottaa robotin hallintaansa antamalla komennon suorittaa action()
     * metodi tapauksessa, jossa kosketussensorin nappi vapautuu.
     * @return !touch.isPressed()
     */
    public boolean takeControl() {
        return !touch.isPressed();
    }
    /**
     * Korkeimman prioriteetin behavior, joten suppressia ei kutsuta missään
     * tilanteessa.
     */
    public void suppress() {
    }
    /**
     * Suorittaa metodin vaaratilastonPaivitys ja kirjoittaa
     * näytölle "Apuva!", peruuttaa nopealla vauhdilla taaksepäin
     * ja kääntyy osoittamaan uuteen suuntaan.
     */
    public void action() {
        Motor.A.stop();
	Motor.C.stop();
        vaaratilastonPaivitys();
	LCD.clear();
	LCD.drawString("Apuva!",0,5);
	Motor.A.setSpeed(400);
	Motor.C.setSpeed(400);
	Motor.A.rotate(700,true);
	Motor.C.rotate(700);
	Motor.A.rotate(-360,true);
	Motor.C.rotate(360);
	Motor.A.setSpeed(250);
	Motor.C.setSpeed(250);
    }
    /**
    * Päivittää data.dat tiedostoon vaarallisten tilanteiden määrän.
    * Aluksi hakee tiedostosta nykyisten vaaratilanteiden määrän,
    * tämän jälkeen lisää määrään yhden tilanteen, ja tallettaa uuden
    * määrän tiedostoon.
    */
    public void vaaratilastonPaivitys(){
        File data = new File("data.dat");
	int luku = 0;
        try{
            InputStream is = new FileInputStream(data);
            DataInputStream din = new DataInputStream(is);
	    if(is.available()>3)
                luku=din.readInt();
		din.close();
        }
	catch(IOException ioe){
            System.err.println("Read Exception");
	}
	try{
            FileOutputStream out = new FileOutputStream(data);
            DataOutputStream dataOut = new DataOutputStream(out);
            int x = luku+1;
            dataOut.writeInt(x);
            out.flush();
            out.close();
	}
	catch(IOException e){
            System.err.println("Failed to write to output stream");
	}
    }
    /**
     * Kosketussensori.
     */
    private TouchSensor touch;
}
/**
 * DriveForward
 *
 * Tässä ohjelmassa tämä on se behavior, jota suoritetaan alustavasti koko ajan,
 * ja joka sitten keskeytyy muiden kahden behaviorin toimesta tilanteen sitä
 * vaatiessa.
 * Kyseinen luokka ajattaa robottia vain eteenpäin.
 *
 * @author Jasu Viding
 */
class DriveForward implements Behavior {
    /**
     * _suppressed alustavasti false.
     */
    private boolean _suppressed = false;
    /**
     * takeControl() haluaa aina kontrollin, sillä return on koko ajan true.
     * @return true;
     */
    public boolean takeControl() {
        return true;
    }
    /**
     * Asettaa _suppressed arvon trueksi, jos jokin muu behavior astuu voimaan.
     */
    public void suppress() {
        _suppressed = true;
    }
    /**
     * Kirjoittaa näytölle "Tyonnan." ja ajattaa robottia eteenpäin, kunnes
     * jokin muu behavior astuu voimaan ja _suppressed muuttuu trueksi.
     */
    public void action() {
        _suppressed = false;
        LCD.clear();
        LCD.drawString("Tyonnan.", 0, 5);
        Motor.A.backward();
        Motor.C.backward();
        while (!_suppressed)
            Thread.yield();
        Motor.A.stop();
        Motor.C.stop();
    }
}
/**
 * DetectObject
 *
 * Jos robotin edessä ei ole mitään, laittaa robotin pyörimään hitaasti paikallaan,
 * kunnes ultraäänisensori havaitsee edessä alle 100cm päässä olevan kappaleen.
 *
 * @author Jasu Viding
 */
class DetectObject implements Behavior {
    /**
     * Alustaa ultraäänisensorin luokan käyttöön.
     */
    public DetectObject() {
        sonar = new UltrasonicSensor(SensorPort.S1);
    }
    /**
     * Ultraäänisensori lähettää koko ajan tiedustelua löytyykö edestä mitään.
     * Jos edessä ja alle 100cm päässä havaitaan jotain, niin antaa käskyn mennä
     * metodiin action().
     * @return sonar.getDistance() > 100;
     */
    public boolean takeControl() {
        sonar.ping();
        return sonar.getDistance() > 100;
    }
    /**
     * Korkeimman prioriteetin behavior, joten suppressia ei kutsuta missään
     * tilanteessa.
     */
    public void suppress() {
    }
    /**
     * Pyörittää robottia hitaalla nopeudella paikallaan ja kirjoittaa näytölle
     * "Etsin.".
     */
    public void action() {
        Motor.A.setSpeed(100);
	Motor.C.setSpeed(100);
	LCD.clear();
	LCD.drawString("Etsin.", 0, 5);
	while(sonar.getDistance()>100){
            Motor.A.backward();
	    Motor.C.forward();
	}
	Motor.A.stop();
	Motor.C.stop();
	Motor.A.setSpeed(250);
	Motor.C.setSpeed(250);
    }
    /**
     * Ultraäänisensori.
     */
    private UltrasonicSensor sonar;
}