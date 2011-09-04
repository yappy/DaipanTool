#pragma once

#include "const.h"

namespace th123data {

inline BYTE offset(int player) {
	const BYTE OFFSET[2] = {
		0x0c,
		0x10
	};
	return OFFSET[player];
}

/*
inline DWORD hpoffset(int player) {
	const DWORD HPOFFSET[2] = {
		0x04A4,
		0x059C
	};
	return HPOFFSET[player];
}
*/

inline DWORD charaoffset(int player) {
	DWORD read;
	DWORD battle, character;
	ReadProcessMemory(GetCurrentProcess(), (LPCVOID)SWRS_ADDR_PBATTLEMGR, &battle, 4, &read);
	ReadProcessMemory(GetCurrentProcess(), (LPCVOID)(battle + offset(player)), &character, 4, &read);
	return character;
}

enum {
	STATE_INITIALIZING = 0,
	STATE_TITLE = 2,
	STATE_CHARSEL = 3,
	STATE_BATTLE = 5,
	STATE_LOADING = 6,
	STATE_NET_SERVER_CHARSEL = 8,
	STATE_NET_CLIENT_CHARSEL = 9,
	STATE_NET_SERVER_LOADING = 10,
	STATE_NET_CLIENT_LOADING = 11,
	STATE_NET_WATCH_LOADING  = 12,
	STATE_NET_SERVER_BATTLE = 13,
	STATE_NET_CLIENT_BATTLE = 14,
	STATE_NET_WATCH_BATTLE  = 15,
	STATE_STORY_ARCADE_CHARSEL = 16,
};

//状態番号を返す
inline BYTE stateID() {
	BYTE state;
	DWORD read;
	ReadProcessMemory(GetCurrentProcess(), (LPCVOID)SWRS_ADDR_SCENEID, &state, 1, &read);
	return state;
}

// ポーズ中かを返す
inline BYTE menuMode() {
	BYTE  state;
	DWORD read;
	ReadProcessMemory(GetCurrentProcess(), (LPCVOID)SWRS_ADDR_PAUSE_MENU, &state, 1, &read);
	return state;
}

//ヒットタイプを返す
inline BYTE hitType(int player) {
	BYTE type;
	DWORD read;
	DWORD character = charaoffset(player);
	ReadProcessMemory(GetCurrentProcess(), (LPCVOID)(character + 0x4AD), &type, 1, &read);
	return type;
}

//対戦モードを返す
inline BYTE comMode() {
	BYTE state;
	DWORD read;
	ReadProcessMemory(GetCurrentProcess(), (LPCVOID)SWRS_ADDR_COMMMODE, &state, 1, &read);
	return state;
}

//リミット取得
inline BYTE limit(int player) {
	BYTE limit;
	DWORD read;
	DWORD character = charaoffset(player);
	ReadProcessMemory(GetCurrentProcess(), (LPCVOID)(character + 0x4B8), &limit, 1, &read);
	return limit;
}

//ステージ番号を返す
inline BYTE stageID() {
	BYTE stage;
	DWORD read;
	ReadProcessMemory(GetCurrentProcess(), (LPCVOID)SWRS_ADDR_STAGEID, &stage, 1, &read);
	return stage;
}

//スペル発動中か
// +48A?
// +54E?
// スミス氏によると+54Fらしい
inline bool isSpellTime(int player) {
	BYTE  time;
	DWORD read;
	DWORD character = charaoffset(player);
	ReadProcessMemory(GetCurrentProcess(), (LPCVOID)(character + 0x54F), &time, 1, &read);
	return !time;
}

//ダメージ量取得
inline WORD damage(int player) {
	WORD dam;
	DWORD read;
	DWORD character = charaoffset(player);
	ReadProcessMemory(GetCurrentProcess(), reinterpret_cast<LPCVOID>(character + 0x4B6), &dam, 2, &read);
	return dam;
}

//ＨＰ取得
inline WORD playerHP(int player) {
	WORD hp;
	DWORD read;
	DWORD character = charaoffset(player);
	ReadProcessMemory(GetCurrentProcess(), reinterpret_cast<LPCVOID>(character + 0x184), &hp, 2, &read);
	return hp;
}

//プレイヤー勝利数取得
inline BYTE playerWin(int player) {
	BYTE  win;
	DWORD read;
	DWORD character = charaoffset(player);
	ReadProcessMemory(GetCurrentProcess(), reinterpret_cast<LPCVOID>(character + 0x573), &win, 1, &read);
	return win;
}

//プレイヤーHP設定
inline void setPlayerHP(int player, WORD hp) {
	DWORD read;
	DWORD character = charaoffset(player);
	WriteProcessMemory(GetCurrentProcess(), reinterpret_cast<LPVOID>(character + 0x184), &hp, 2, &read);
}

inline void setPlayerWin(int player, BYTE win) {
	DWORD read;
	DWORD character = charaoffset(player);
	WriteProcessMemory(GetCurrentProcess(), reinterpret_cast<LPVOID>(character + 0x573), &win, 1, &read);
}

inline BYTE comboCount(int player) {
	BYTE  cnt;
	DWORD read;
	DWORD character = charaoffset(player);
	ReadProcessMemory(GetCurrentProcess(), reinterpret_cast<LPCVOID>(character + 0x4B4), &cnt, 1, &read);
	return cnt;
}

}
