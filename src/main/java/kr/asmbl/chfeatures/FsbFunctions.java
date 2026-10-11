package kr.asmbl.chfeatures;

import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.MSVersion;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.constructs.CBoolean;
import com.laytonsmith.core.constructs.CByteArray;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.CVoid;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.exceptions.CRE.CRECastException;
import com.laytonsmith.core.exceptions.CRE.CREFormatException;
import com.laytonsmith.core.exceptions.CRE.CRELengthException;
import com.laytonsmith.core.exceptions.CRE.CREPlayerOfflineException;
import com.laytonsmith.core.exceptions.CRE.CREPluginInternalException;
import com.laytonsmith.core.exceptions.CRE.CREThrowable;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.AbstractFunction;
import com.laytonsmith.core.natives.interfaces.Mixed;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * FiguraFSB(서버↔Figura 아바타 통신 플러그인) 연동 함수.
 *
 * 중요: 이 파일의 @api 클래스에는 FSB 타입을 절대 직접 참조하지 않는다 (시그니처 포함).
 * FSB API 호출은 전부 FsbBridge에 위임한다.
 */
public class FsbFunctions {

	public static String docs() {
		return "Functions for communicating with Figura avatar scripts through the FiguraFSB plugin.";
	}

	private static void checkFsb(Target t) {
		if(!Bukkit.getPluginManager().isPluginEnabled("FiguraFSB")) {
			throw new CREPluginInternalException("FiguraFSB is not installed or enabled on this server.", t);
		}
	}

	private static byte[] toBytes(Mixed value, Target t) {
		if(value instanceof CByteArray bytes) {
			return bytes.asByteArrayCopy();
		}
		if(value instanceof CString) {
			return value.val().getBytes(StandardCharsets.UTF_8);
		}
		throw new CRECastException("Expecting a byte_array or string as packet data.", t);
	}

	private abstract static class FsbFunction extends AbstractFunction {

		@Override
		public boolean isRestricted() {
			return true;
		}

		@Override
		public Boolean runAsync() {
			return false;
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	@api
	public static class fsb_send_packet extends FsbFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPluginInternalException.class, CREPlayerOfflineException.class,
					CRECastException.class, CREFormatException.class, CRELengthException.class};
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			checkFsb(t);
			Player receiver = (Player) Static.GetPlayer(args[0], t).getHandle();
			UUID avatarOwner = null;
			String packetName;
			Mixed dataArg;
			if(args.length == 4) {
				// receiver, avatarOwnerUUID, packetName, data
				avatarOwner = Static.GetUUID(args[1], t);
				packetName = args[2].val();
				dataArg = args[3];
			} else {
				// receiver, packetName, data
				packetName = args[1].val();
				dataArg = args[2];
			}
			FsbBridge.sendPacket(receiver, avatarOwner, packetName, toBytes(dataArg, t), t);
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "fsb_send_packet";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{3, 4};
		}

		@Override
		public String docs() {
			return "void {receiver, packetName, data | receiver, avatarOwnerUUID, packetName, data} Sends a named"
					+ " server packet to a Figura avatar script on the receiver's client, through the FiguraFSB"
					+ " plugin. The 3-argument form targets the receiver's own (host) avatar; the 4-argument form"
					+ " targets a specific avatar loaded on the receiver's client (requires non-host packet support,"
					+ " see the FSB allowNonHostPackets config for replies)."
					+ " The avatar script receives it via server_packets[packetName] = function(buffer) ... end."
					+ " Data may be a byte_array or a string (sent as UTF-8 bytes), at most 32746 bytes."
					+ " Silently does nothing if the receiver has no active FSB session"
					+ " (check with fsb_is_connected).";
		}
	}

	@api
	public static class fsb_register_packet extends FsbFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPluginInternalException.class};
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			checkFsb(t);
			FsbBridge.registerListener(args[0].val());
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "fsb_register_packet";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1};
		}

		@Override
		public String docs() {
			return "void {packetName} Registers a listener for a named FSB server packet, so that packets sent by"
					+ " avatar scripts via server_packets:sendPacket(packetName, buffer) fire the fsb_server_packet"
					+ " event. FSB allows only one listener per name across ALL plugins — registering a name used"
					+ " by another plugin replaces its listener, so namespace your packet names (e.g. 'chf:mypacket')."
					+ " Registration persists until the server restarts or the name is registered again;"
					+ " call this on script load (it is safe to call repeatedly).";
		}
	}

	@api
	public static class fsb_is_connected extends FsbFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPluginInternalException.class, CREPlayerOfflineException.class};
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			checkFsb(t);
			Player player = (Player) Static.GetPlayer(args[0], t).getHandle();
			return CBoolean.get(FsbBridge.isConnected(player.getUniqueId()));
		}

		@Override
		public String getName() {
			return "fsb_is_connected";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1};
		}

		@Override
		public String docs() {
			return "boolean {player} Returns whether the player has completed the FiguraFSB handshake, meaning"
					+ " they run a Figura client with FSB support and can send/receive server packets and pings.";
		}
	}
}
