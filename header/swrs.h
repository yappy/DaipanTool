#ifndef SWRS_H_INCLUDED
#define SWRS_H_INCLUDED

// ---------------- ここからテンプレ ----------------

#ifdef SWRS_USES_HASH
extern const BYTE TARGET_HASH[16];
__declspec(selectany) const BYTE TARGET_HASH[16] =
{ 0x26, 0x8a, 0xfd, 0x82, 0x76, 0x90, 0x3e, 0x16, 0x71, 0x6c, 0x14, 0x29, 0xc6, 0x95, 0x9c, 0x9d };
#endif

// DWORD書き換え
__inline
DWORD TamperDword(DWORD addr, DWORD target)
{
	DWORD old = *reinterpret_cast<PDWORD>(addr);
	*reinterpret_cast<PDWORD>(addr) = target;
	return old;
}

// DWORD加算
__inline
DWORD TamperDwordAdd(DWORD addr, DWORD delta)
{
	DWORD old = *reinterpret_cast<PDWORD>(addr);
	*reinterpret_cast<PDWORD>(addr) += delta;
	return old;
}

// NEAR JMPオペランド書き換え
__inline
DWORD TamperNearJmpOpr(DWORD addr, DWORD target)
{
	DWORD old = *reinterpret_cast<PDWORD>(addr + 1) + (addr + 5);
	*reinterpret_cast<PDWORD>(addr + 1) = target - (addr + 5);
	return old;
}

// NEAR JMP書き換え
__inline
void TamperNearJmp(DWORD addr, DWORD target)
{
	*reinterpret_cast<PBYTE>(addr + 0) = 0xE9;
	TamperNearJmpOpr(addr, target);
}

// NEAR CALL書き換え
__inline
void TamperNearCall(DWORD addr, DWORD target)
{
	*reinterpret_cast<PBYTE>(addr + 0) = 0xE8;
	TamperNearJmpOpr(addr, target);
}

// フォントディスクリプタ
#pragma pack(push, 4)
struct SWRFONTDESC {
	char FaceName[0x100];
	BYTE R1;
	BYTE R2;
	BYTE G1;
	BYTE G2;
	BYTE B1;
	BYTE B2;
	LONG Height;
	LONG Weight;
	BYTE Italic;
	BYTE Shadow;
	BYTE UseOffset;
	DWORD BufferSize;
	DWORD OffsetX;
	DWORD OffsetY;
	DWORD CharSpaceX;
	DWORD CharSpaceY;
};

// コンパクト文字列
struct SWRSTR {
	union {
		char  str[16];
		char* pstr;
	};
	size_t length;

	operator char *() {
		return length > 15 ? pstr : str;
	}
};

// エフェクトマネージャインターフェース
struct __declspec(novtable) IEffectManager {
	virtual ~IEffectManager() {}
	virtual void LoadPattern(LPCSTR fileName, int) = 0;
	virtual void ClearPattern() = 0;
	virtual void AppendRegion(int arg_0, float arg_4, float arg_8, char arg_c, char arg_10, int arg_14) = 0;
	virtual void ClearRegion() = 0;
};

// ファイルリーダインターフェース
struct __declspec(novtable) IFileReader {
	virtual ~IFileReader() {}
	virtual bool Read(LPVOID lpBuffer, DWORD nNumberOfBytesToRead) = 0;
	virtual DWORD GetReadLength() = 0;
	virtual LONG Seek(LONG lDistanceToMove, DWORD dwMoveMethod) = 0;
	virtual DWORD GetLength() = 0;
};

// よくわからないもの
struct UnknownF {
	void *Unknown[3];
	float Aaxis;
	float Baxis;
};

// static_assert
template<bool value>
struct static_assert { typedef struct assert_failed Type; };
template<>
struct static_assert<true> { typedef void* Type; };

// union_cast
template<typename TDest, typename TSrc>
__forceinline 	
TDest union_cast(TSrc src) {
	static_assert<sizeof(TDest) == sizeof(TSrc)>::Type 
		size_mismatch = 0;
	union {  TDest dst; TSrc src; } c;
	c.src = src;
	return c.dst;
}

// thiscall
class C {};
#define Ccall(p,f,r,T) (((C*)p)->*union_cast<r(C::*)T>(f))
// フォントオブジェクトメソッド
#define SWRFont_Create(p) \
	Ccall(p,0x00411170,void,())()
#define SWRFont_Destruct(p) \
	Ccall(p,0x00411200,void,())()
#define SWRFont_SetIndirect(p, pdesc) \
	Ccall(p,0x004112E0,void,(void*))(pdesc)

// テクスチャマネージャメソッド
#define CTextureManager_LoadTexture(p, ret, path, unk1, unk2) \
	Ccall(p,0x00404CC0,int*,(int*, LPCSTR, void*, void*))(ret, path, unk1, unk2)
#define CTextureManager_CreateTextTexture(p, ret, str, pdesc ,width ,height, p1, p2) \
	Ccall(p,0x00404D30,int*,(int*, LPCSTR, void*, int, int, int*, int*))(ret, str, pdesc, width, height, p1, p2)
#define CTextureManager_Remove(p, id) \
	Ccall(p,0x00404DA0,void*,(int))(id)
#define CTextureManager_SetTexture(p, id, stage) \
	Ccall(p,0x00404E20,void,(int, int))(id, stage)
#define CTextureManager_GetSize(p, w, h) \
	Ccall(p,0x00404E90,void,(int*, int*))(w, h)
// テクスチャマネージャメソッド(ハンドルマネージャからの継承)
#define CTextureManager_Get(p, id) \
	((IDirect3DTexture9**)CHandleManager_Get(p, id))
#define CTextureManager_Allocate(p, id) \
	((IDirect3DTexture9**)CHandleManager_Allocate(p, id))
#define CTextureManager_Deallocate(p, id) \
	CHandleManager_Deallocate((void *)p, id)

// ハンドルマネージャメソッド
#define CHandleManager_Get(t, p, id) \
	Ccall(p,0x00402770,t*,(int))(id)
inline __declspec(naked) void**
Thunk_CHandleManager_Allocate(void *p, int *ret) {
	// thiscallなのに落ちると思ったらediを使っていたでござる　の巻
	__asm push edi
	__asm mov edi, [esp+8]
	__asm push [esp+12]
	__asm mov eax, 0x00402600
	__asm call eax
	__asm pop edi
	__asm retn
}
#define CHandleManager_Allocate(p, ret) \
	Thunk_CHandleManager_Allocate(p, ret)
inline __declspec(naked) void
Thunk_CHandleManager_Deallocate(void *p, int id) {
	 // thiscall(中略)eaxを使っていたでござる　の巻
	__asm mov eax, [esp+4]
	__asm push [esp+8]
	__asm mov ecx, 0x004175D0
	__asm call ecx
	__asm retn
}
#define CHandleManager_Deallocate(p, id) \
	Thunk_CHandleManager_Deallocate(p, id)

// レンダラメソッド
#define CRenderer_Begin(p)  \
	Ccall(p,0x00401000,bool,())()
#define CRenderer_End(p) \
	Ccall(p,0x00401040,void,())()
/*
// セレクトエフェクトマネージャメソッド
#define CSelectEffectManager_Create_Address 0x420CE0
#define CSelectEffectManager_Free_Address   0x4221F0
#define CSelectEffectManager_Create(p) \
	Ccall(p, CSelectEffectManager_Create_Address, void, ())()
*/

// インプットマネージャメソッド
#define CInputManager_ReadReplay(p, name) \
	Ccall(p, 0x0042E5C0, bool,(char *))(name)

// ベクタオブジェクト
#define Vector_Create \
	((void (__stdcall *)(void *, size_t, size_t, int, int))0x00811B8B)
#define Vector_Destruct \
	((void (__stdcall *)(void *, size_t, size_t, int))0x00811B28)

// システムキーワンショット
#define CheckKeyOneshot  \
	((bool(*)(int, int, int, int))0x0043D000)
// パレットロード
#define LoadPackagePalette(pflag, name, pal, bpp) \
	Ccall(pflag, 0x00408B40, void, (LPCSTR, void *, int))(name, pal, bpp)

// データロード
#define LoadPackageFile(ppfile, name) \
	Ccall(ppfile, 0x0040CD90, bool, (LPCSTR))(name)

// SE再生
#define PlaySEWaveBuffer \
	((void (*)(int id))0x0043D3B0)

// キャラクタ略称取得
#define GetCharacterAbbr \
	((LPCSTR (__cdecl *)(int id))0x0043E5E0)

// バトルモード設定
#define SetBattleMode \
	((void (__cdecl *)(int comm, int sub))0x0043DBC0)

// 角度cos
#define DegreeCosine \
    ((float (__cdecl *)(int deg))0x00409210)

// new/delete
#define SWRS_new(t) ((void*(__cdecl *)(size_t))0x008116DC)(t)
#define SWRS_delete(p) ((void(__cdecl *)(void*))0x008111FA)(p)

// テクスチャマネージャ
// CHandleManager<IDirect3DTexture *>
#define g_textureMgr ((void *)0x0088CEE8)
// Direct3Dデバイス
// IDirect3DDevice9*
#define g_pd3dDev    (*((IDirect3DDevice9**)0x0088DE10))
// レンダラ
// CRenderer
#define g_renderer   ((void *)0x00883B4C)
// ネットワークオブジェクト
// CNetworkServer/CNetworkClient
#define g_pnetObject (*(char**)0x00885680)
// プロファイル名
// char *
#define g_pprofP1 ((char*)(g_pnetObject + 0x04))
#define g_pprofP2 ((char*)(g_pnetObject + 0x24))
// UDPネットワークオブジェクト
// CNetworkBase
#define g_pnetUdp    (g_pnetObject + 0x3BC)
// ピア情報
// vector<SWRClientInfo> ?
#define g_psvClients (g_pnetUdp + 0x10C)
// サーバアドレス
// in_addr
#define g_ptoAddr    (g_pnetUdp + 0x3C)
// バトルマネージャ
// CBattleManager *
#define g_pbattleMgr (*(void **)0x008855C4)
// インフォマネージャ
// CInfoManager *
#define g_pinfoMgr   (*(void **)0x008855C8)
// モード
// int
#define g_commMode   (*(DWORD*)0x00885670)
#define g_subMode    (*(DWORD*)0x00885668)
#define g_menuMode   (*(DWORD*)0x0086FA94)
// シーンID
// DWORD
#define g_sceneIdNew (*(DWORD*)0x0088D020)
#define g_sceneId    (*(DWORD*)0x0088D024)
// コンバートデータ利用フラグ
// bool
#define g_useCVxData (*(bool*)0x0088D028)
// パレットオブジェクト
// void *
#define g_paletter   (*(void **)0x00883B88)
// インプットマネージャ
// CInputManager ?
#define g_inputMgr   ((void *)0x008856F8)
// インプットマネージャクラスタ
// CInputManagerCluster
#define g_inputMgrs  ((void *)0x00887228)
// キャラクタID
// int
#define g_leftCharID (*(int*)0x00886CF0)
#define g_rightCharID (*(int*)0x00886D10)
// argc/argv
#define __argc       (*(int*)0x00887B38)
#define __argv       (*(char***)0x00887B3C)

// 仮想関数テーブル
#define vtbl_CLogo              0x00846738
#define vtbl_Opening            0x008467D4
#define vtbl_CLoading           0x0084665C
#define vtbl_CTitle             0x00846FA4
#define vtbl_CSelect            0x00846D18
#define vtbl_CSelectScenario    0x00846DD0
#define vtbl_CBattle            0x00846490
#define vtbl_Ending             0x0084660C
#define vtbl_CSelectSV          0x008464CC
#define vtbl_CLoadingSV         0x008464EC
#define vtbl_CBattleSV          0x00846508
#define vtbl_CSelectCL          0x00846524
#define vtbl_CLoadingCL         0x00846544
#define vtbl_CBattleCL          0x00846560
#define vtbl_CLoadingWatch      0x00847178
#define vtbl_CBattleWatch       0x0084657C
#define vtbl_CBattleManager     0x008478EC

// クラス構築関数caller
#define CLogo_Creater           0x0041DB07
#define Opening_Creater         0x0041DB41
#define CLoading_Creater        0x0041DB7B
#define CTitle_Creater          0x0041DBB5
#define CSelect_Creater         0x0041DBEF
#define CSelectScenario_Creater 0x0041DC29
#define CBattle_Creater         0x0041DC60
#define Ending_Creater          0x0041DC9A
#define CSelectSV_Creater       0x0041DCD4
#define CLoadingSV_Creater      0x0041DD0E
#define CBattleSV_Creater       0x0041DD45
#define CSelectCL_Creater       0x0041DD7F
#define CLoadingCL_Creater      0x0041DDB9
#define CBattleCL_Creater       0x0041DDF0
#define CLoadingWatch_Creater   0x0041DE26
#define CBattleWatch_Creater    0x0041DE59
#define CBattleManager_Creater  0x00438B30

// クラスサイズオペランド
#define CLogo_Size              (*(DWORD*)0x0041DAE5)
#define Opening_Size            (*(DWORD*)0x0041DB1F)
#define CLoading_Size           (*(DWORD*)0x0041DB59)
#define CTitle_Size             (*(DWORD*)0x0041DB93)
#define CSelect_Size            (*(DWORD*)0x0041DBCD)
#define CSelectScenario_Size    (*(DWORD*)0x0041DC07)
#define CBattle_Size            (*(BYTE *)0x0041DC41)
#define Ending_Size             (*(DWORD*)0x0041DC78)
#define CSelectSV_Size          (*(DWORD*)0x0041DCB2)
#define CLoadingSV_Size         (*(DWORD*)0x0041DCEC)
#define CBattleSV_Size          (*(BYTE *)0x0041DD26)
#define CSelectCL_Size          (*(DWORD*)0x0041DD5D)
#define CLoadingCL_Size         (*(DWORD*)0x0041DD97)
#define CBattleCL_Size          (*(BYTE *)0x0041DDD1)
#define CLoadingWatch_Size      (*(DWORD*)0x0041DE08)
#define CBattleWatch_Size       (*(BYTE *)0x0041DE3E)
#define CBattleManager_Size     (*(DWORD*)0x00438B12)

// セクションサイズ
#define text_Offset  0x00401000
#define text_Size    0x00445000
#define rdata_Offset 0x00846000
#define rdata_Size   0x00029000
#define data_Offset  0x0086F000
#define data_Size    0x00015000

// ---------------- ここまでテンプレ ----------------
#endif
