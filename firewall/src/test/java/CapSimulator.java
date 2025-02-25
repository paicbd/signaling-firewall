import java.io.InputStream;
// import java.util.Scanner;
import com.paic.esg.helpers.ExtendedResource;
import com.paic.esg.impl.settings.XmlConfiguration;
import com.paic.esg.impl.settings.cap.CapSettings;
import com.paic.esg.impl.settings.m3ua.M3uaSettings;
import com.paic.esg.impl.settings.sccp.SccpSettings;
import com.paic.esg.impl.settings.sctp.SctpSettings;
import com.paic.esg.impl.settings.tcap.TcapSettings;
import com.paic.esg.network.layers.CapLayer;
import com.paic.esg.network.layers.M3uaLayer;
import com.paic.esg.network.layers.SccpLayer;
import com.paic.esg.network.layers.SctpLayer;
import com.paic.esg.network.layers.TcapLayer;
import com.paic.prototype.camel.HplmnScpPrototype;
import com.paic.prototype.camel.VplmnStpPrototype;
import org.apache.log4j.Logger;

/**
 * CapSimulator
 */
public class CapSimulator {

  private static final Logger logger = Logger.getLogger(CapSimulator.class);
  private XmlConfiguration configuration;

  private CapLayer getCapLayer(int index) {
    try {
      logger.info("Initializing SCTP layer...");
      SctpSettings sctpSettings = (SctpSettings) configuration.getLayerSettings("sctp" + index);
      SctpLayer sctp = new SctpLayer(sctpSettings);

      logger.info("Initializing M3UA layer...");
      M3uaSettings m3uaSettings = (M3uaSettings) configuration.getLayerSettings("m3ua" + index);
      M3uaLayer m3ua = new M3uaLayer(m3uaSettings, sctp);

      logger.info("Initializing SCCP layer...");
      SccpSettings sccpClientSettings =
          (SccpSettings) configuration.getLayerSettings("sccp" + index);
      SccpLayer sccp = new SccpLayer(sccpClientSettings, m3ua);

      logger.info("Initializing TCAP layer...");
      TcapSettings tcapSettings = (TcapSettings) configuration.getLayerSettings("tcap" + index);
      TcapLayer tcap = new TcapLayer(tcapSettings, sccp);

      logger.info("Initializing CAP layer...");
      CapSettings capSettings = (CapSettings) configuration.getLayerSettings("cap" + index);
      return new CapLayer(capSettings, tcap);
    } catch (Exception e) {
      logger.error(e.getMessage());
    }
    return null;
  }

  private void initialize() {
    try {
      InputStream is = new ExtendedResource("cap-simulator-config.xml").getAsStream();
      this.configuration = new XmlConfiguration(is);

      logger.info("Initializing the channel layers.");
      // CapLayer[] caplayers = IntStream.range(0, 3).mapToObj(index -> {
      // return getCapLayer(index);
      // }).filter(cap -> cap != null).toArray(size -> new CapLayer[size]);
      CapLayer[] caplayers = new CapLayer[4];
      for (int i = 0; i < 4; i++) {
        caplayers[i] = getCapLayer(i);
      }

      // get the transport layer name.
      // VPLMN
      VplmnStpPrototype vplmnStpPrototype = new VplmnStpPrototype(caplayers[0].getCapProvider(),
          caplayers[0].getCapProvider().getCAPParameterFactory(), caplayers[1].getCapProvider(),
          caplayers[1].getCapProvider().getCAPParameterFactory(), caplayers[2].getCapProvider(),
          caplayers[2].getCapProvider().getCAPParameterFactory());
      logger.info("VPLMN started" + vplmnStpPrototype.toString());

      // HPLMN
      HplmnScpPrototype hplmnScpPrototype = new HplmnScpPrototype(caplayers[3].getCapProvider(),
          caplayers[3].getCapProvider().getCAPParameterFactory());
      logger.info("HPLMN started" + hplmnScpPrototype.toString());
      Thread.sleep(5000);

      for (int i = 0; i < 120; i++) {
        try {
          Thread.sleep(8000);
          vplmnStpPrototype.sendInitialDPRequest();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      logger.info("DONE");
      // Scanner scanner = new Scanner(System.in);
      // do {
      // try {
      // vplmn.sendInitialDPRequest();
      // } catch (Exception err) {
      // err.printStackTrace();
      // }
      // reply = scanner.nextLine();

      // } while (!reply.equalsIgnoreCase("quit"));

      // scanner.close();
      // this.simulate();
    } catch (Exception ex) {
      logger.error("Simulation Error: ", ex);
    }
  }

  public CapSimulator() {

  }

  public static void main(String[] args) {
    CapSimulator vplmn = new CapSimulator();
    vplmn.initialize();;
  }

}
