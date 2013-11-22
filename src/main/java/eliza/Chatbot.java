package eliza;

import iw7i.messages.chat.ChatMessage;
import iw7i.messages.chat.ChatMessageDecoder;
import iw7i.messages.chat.ChatMessageEncoder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

/**
 * @author Lukas Gedvilas
 * 
 */
@ClientEndpoint(encoders = ChatMessageEncoder.class, decoders = ChatMessageDecoder.class)
public class Chatbot {
	private String name, room_address;
	private ElizaMain eliza;
	private Logger log;

	public Chatbot(String name, String room_address) {
		this.name = name;
		this.room_address = room_address;
		this.eliza = new ElizaMain();

	}

	@OnOpen
	public void onOpen(Session session) {
		eliza.readScript(true, "script");
		ChatMessage welcomeMessage = new ChatMessage();
		welcomeMessage.setMessage("Hello!");
		welcomeMessage.setReceived(new Date());
		welcomeMessage.setSender(name);
		log.info("Connected to " + room_address);
		try {
			session.getBasicRemote().sendObject(welcomeMessage);
		} catch (IOException | EncodeException e) {
			log.log(Level.WARNING, "welcome message failed", e);
		}
	}

	@OnMessage
	public void onMessage(final Session session, final ChatMessage chatMessage) {
		// comprobamos que el mensaje que llega no es el generado por nuestro bot
		if (!chatMessage.getSender().equals(name)) {
			log.info("message " + chatMessage);
			ChatMessage responseMessage = new ChatMessage();
			responseMessage.setMessage(eliza.processInput(chatMessage
					.getMessage()));
			responseMessage.setReceived(new Date());
			responseMessage.setSender(name);
			log.info("bot message " + responseMessage);
			try {
				session.getBasicRemote().sendObject(responseMessage);
			} catch (IOException | EncodeException e) {
				log.log(Level.WARNING, "onMessage failed", e);
			}
		}
	}

	@OnClose
	public void onClose(Session session, CloseReason closeReason) {
		log.info(String.format("Session %s was closed because of %s",
				session.getId(), closeReason));
	}

	/**
	 * @param args
	 *            ${name} ${room_address} where ${name} is the name used by the
	 *            chatbot when it impersonates a user and ${room_address} is the
	 *            ws: URL of the room.
	 */
	public static void main(String[] args) {
		// comprobamos que se han proporcionado 2 argumentos al programa
		if (args.length != 2) {
			// si no se han proporcionado 2 argumentos, se informa de ello y se
			// termina la ejecución
			System.out.println("Incorrect number of arguments");
			System.out.println("Try to run \"java -jar chatbot.jar ${name}"
					+ " ${room_address}\"");
			System.out
					.println("where ${name} is the name used by the chatbot when it "
							+ "impersonates a user and ${room_address} is the "
							+ "ws: URL of the room.");
		} else {
			Chatbot chatbot = new Chatbot(args[0], args[1]);
			chatbot.configureLogger();
			chatbot.connectToChat();
		}

	}

	private void configureLogger() {
		log = Logger.getLogger(getClass().getName());
		FileHandler fh;
		try {
			fh = new FileHandler("chatbot_log.log");
			log.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();
			fh.setFormatter(formatter);
			log.setUseParentHandlers(false); //para que no muestre los logs en la consola
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
	}

	private void connectToChat() {
		try {
			ContainerProvider.getWebSocketContainer().connectToServer(this,
					new URI(room_address));
		} catch (DeploymentException | IOException | URISyntaxException e) {
			log.severe("Could not connect to " + room_address);
			throw new RuntimeException(e);
		}
	}
}
