/*
 * Created by Matt Zafeiriou (Ματθαιος Ζαφειριου)
 * 29/04/2020 @ 20:15
 */

#define _WINSOCK_DEPRECATED_NO_WARNINGS
#include <iostream>
#include <conio.h>
#include <winsock2.h>
#include <string>
#include <vector>
#pragma comment(lib, "Ws2_32.lib")

int port = 15000;
const char* address = "127.0.0.1";

void sendMessage(const char* msg, SOCKET u_sock) {
	const char* mymsg = msg;
	int length = strlen(mymsg);
	//Cast the integer to char and send it
	int smsg = send(u_sock, reinterpret_cast<char*>(&length), sizeof(int), 0);
	//Send the actual message
	smsg = send(u_sock, mymsg, strlen(mymsg), 0);
}

int i = 0;
void messagesHandler(SOCKET u_sock) {
	i++;
	std::string msg = "test: ";
	msg.append(std::to_string(i));
	sendMessage(&msg[0], u_sock);
}

void createClient() {
	// creating client
	WSAData version;
	WORD mkword = MAKEWORD(2, 2);
	int what = WSAStartup(mkword, &version);
	if (what != 0) {
		std::cout << "This version is not supported! - \n" << WSAGetLastError() << std::endl;
	}

	SOCKET u_sock = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	if (u_sock == INVALID_SOCKET)
		std::cout << "Creating socket fail\n";

	// server info
	sockaddr_in addr;
	addr.sin_family = AF_INET;
	addr.sin_addr.s_addr = inet_addr(address);
	addr.sin_port = htons(port);

	// connect
	int conn = connect(u_sock, (SOCKADDR*)& addr, sizeof(addr));
	std::cout << "conn value:" << conn << std::endl;
	if (conn == SOCKET_ERROR) {
		std::cout << "Error - when connecting " << WSAGetLastError() << std::endl;
		closesocket(u_sock);
		WSACleanup();
                return;
	}
	std::cout << "Successfully connected to server" << std::endl;
	//Send some message to remote host
	bool sending = true;
	while (sending) {
		messagesHandler(u_sock);
		int newlength;
		//Receive exactly 4 bytes for the length. If not the right length is received, repeat.
		int get = 0;
		while ((get += recv(u_sock, (reinterpret_cast<char*>(&newlength)) + get, 4, 0)) < 4) {}
		std::cout << "Length: " << newlength << std::endl;
		//Create new char array with newlength + 1 so we have a zero terminated string.
		char* newMsg = new char[newlength + 1];
		memset(newMsg, 0, newlength + 1);
		get = 0;
		//Receive the string. If not the right length is received, repeat.
		while ((get += recv(u_sock, newMsg + get, newlength, 0)) < newlength) {}
		std::cout << "Message: " << newMsg << std::endl;

		std::string status = std::string(newMsg);
		if (!status.compare("1")) {
			sending = true;
		}
		else {
			sending = false;
		}
		Sleep(1000);
	}
	closesocket(u_sock);
}

int main(int argc, char* argv[]) {

	createClient();
	return 0;
}
