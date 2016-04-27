package com.service.bluetooth;

import java.util.HashMap;

public class BTConstants
{
    // From the top of ril.cpp
    static final int BT_ERRNO_INVALID_RESPONSE = -1;

    // from RIL_Errno
    static final int SUCCESS = 0;
    static final int TIME_OUT = 1;                 /* If radio did not start or is resetting */
    static final int BT_NOT_AVAILABLE = 2;                 /* If radio did not start or is resetting */
    static final int GENERIC_FAILURE = 3;
    static final int INVALID_ARGUMENT = 102; /* one or more argument is invalid */
    static final int ERR_SERVICE_ALREADY_STARTING = 214;
    static final int ERR_NO_ONGOING_CALL = 250;

    static final int BT_REQUEST_SEND_CMD = 0;

    //GEN
    static final int BT_REQUEST_GVER = 1;
    static final int BT_REQUEST_GLBD = 2;
    static final int BT_REQUEST_GLDN = 3;
    static final int BT_REQUEST_SLDN = 4;
    static final int BT_REQUEST_GRDN = 5;
    static final int BT_REQUEST_SPIN = 6;
    static final int BT_REQUEST_GPIN = 7;
    static final int BT_REQUEST_GPRD = 8;
    static final int BT_REQUEST_DPRD = 9;
    static final int BT_REQUEST_INQU = 10;
    static final int BT_REQUEST_PAIR = 11;
    static final int BT_REQUEST_SCAN = 12;
    static final int BT_REQUEST_EDFU = 13;
    static final int BT_REQUEST_UART = 14;
    static final int BT_REQUEST_SCOD = 15;
    static final int BT_REQUEST_GCOD = 16;
    static final int BT_REQUEST_SPRO = 17;

    //HFP
    static final int BT_REQUEST_HFCONN = 18;
    static final int BT_REQUEST_HFDISC = 19;
    static final int BT_REQUEST_HFANSW = 20;
    static final int BT_REQUEST_HFCHUP = 21;
    static final int BT_REQUEST_HFDIAL = 22;
    static final int BT_REQUEST_HFDTMF = 23;
    static final int BT_REQUEST_HFCTRS = 24;
    static final int BT_REQUEST_HFMCAL = 25;
    static final int BT_REQUEST_HFCLCC = 26;
    static final int BT_REQUEST_HFSVGS = 27;
    static final int BT_REQUEST_HFGVGS = 28;
    static final int BT_REQUEST_HFSVGM = 29;
    static final int BT_REQUEST_HFGVGM = 30;
    static final int BT_REQUEST_HFMUTE = 31;
    static final int BT_REQUEST_HFSCFG = 32;
    static final int BT_REQUEST_HFGCFG = 33;

    //A2DP
    static final int BT_REQUEST_A2DPCONN = 34;
    static final int BT_REQUEST_A2DPDISC = 35;
    static final int BT_REQUEST_A2DPSVGS = 36;
    static final int BT_REQUEST_A2DPGVGS = 37;

    //AVRCP
    static final int BT_REQUEST_AVRCPPLAY = 38;
    static final int BT_REQUEST_AVRCPPAUSE = 39;
    static final int BT_REQUEST_AVRCPSTOP = 40;
    static final int BT_REQUEST_AVRCPFORWARD = 41;
    static final int BT_REQUEST_AVRCPBACKWARD = 42;
    static final int BT_REQUEST_AVRCPVOLUMEUP = 43;
    static final int BT_REQUEST_AVRCPVOLUMEDOWN = 44;
    static final int BT_REQUEST_AVRCPSABSVOL = 45;

    //PBAP
    static final int BT_REQUEST_PBCCONN = 46;
    static final int BT_REQUEST_PBCDISC = 47;
    static final int BT_REQUEST_PBCPULLPB = 48;
    static final int BT_REQUEST_PBCPULLCONT = 49;
    static final int BT_REQUEST_PBCPULLCRT = 50;
    static final int BT_REQUEST_PBCPULLCMT = 51;
    static final int BT_REQUEST_PBCSETPARSE = 52;
    static final int BT_REQUEST_PBCGETPARSE = 53;


    //HID
    static final int BT_REQUEST_HIDCONN = 54;
    static final int BT_REQUEST_HIDDISC = 55;

    //MAP
    static final int BT_REQUEST_MAPCCONN = 56;
    static final int BT_REQUEST_MAPCDISC = 57;
    static final int BT_REQUEST_MAPCGETML = 58;
    static final int BT_REQUEST_MAPCGETCONT = 59;
    static final int BT_REQUEST_MAPCGETMSG = 60;
    static final int BT_REQUEST_MAPCGETCRT = 61;
    static final int BT_REQUEST_MAPCPUSHMSG = 62;
    static final int BT_REQUEST_MAPCCMT = 63;

    //Other
    static final int BT_REQUEST_GPRL = 64;

    //SPP
    static final int BT_REQUEST_SPPCONN = 65;
    static final int BT_REQUEST_SPPDISC = 66;
    static final int BT_REQUEST_SPPDATA = 67;


    static String[] getCommands()
    {
        return new String[]{"",              //BT_REQUEST_SEND_CMD
                "GVER",          //BT_REQUEST_GVER
                "GLBD",          //BT_REQUEST_GLBD
                "GLDN",          //BT_REQUEST_GLDN
                "SLDN",          //BT_REQUEST_SLDN
                "GRDN",          //BT_REQUEST_GRDN
                "SPIN",          //BT_REQUEST_SPIN
                "GPIN",          //BT_REQUEST_GPIN
                "GPRD",          //BT_REQUEST_GPRD
                "DPRD",          //BT_REQUEST_DPRD
                "INQR",          //BT_REQUEST_INQU
                "PAIR",          //BT_REQUEST_PAIR
                "SCAN",          //BT_REQUEST_SCAN
                "EDFU",          //BT_REQUEST_EDFU
                "UART",          //BT_REQUEST_UART
                "SCOD",          //BT_REQUEST_SCOD
                "GCOD",          //BT_REQUEST_GCOD
                "SPRO",          //BT_REQUEST_SPRO
                "HFCONN",        //BT_REQUEST_HFCONN
                "HFDISC",        //BT_REQUEST_HFDISC
                "HFANSW",        //BT_REQUEST_HFANSW
                "HFCHUP",        //BT_REQUEST_HFCHUP
                "HFDIAL",        //BT_REQUEST_HFDIAL
                "HFDTMF",        //BT_REQUEST_HFDTMF
                "HFCTRS",        //BT_REQUEST_HFCTRS
                "HFMCAL",        //BT_REQUEST_HFMCAL
                "HFCLCC",        //BT_REQUEST_HFCLCC
                "HFSVGS",        //BT_REQUEST_HFSVGS
                "HFGVGS",        //BT_REQUEST_HFGVGS
                "HFSVGM",        //BT_REQUEST_HFSVGM
                "HFGVGM",        //BT_REQUEST_HFGVGM
                "HFMUTE",        //BT_REQUEST_HFMUTE
                "HFSCFG",        //BT_REQUEST_HFSCFG
                "HFGCFG",        //BT_REQUEST_HFGCFG
                "A2DPCONN",      //BT_REQUEST_A2DPCONN
                "A2DPDISC",      //BT_REQUEST_A2DPDISC
                "A2DPSVGS",      //BT_REQUEST_A2DPSVGS
                "A2DPGVGS",       //BT_REQUEST_A2DPGVGS
                "AVRCPPLAY",     //BT_REQUEST_AVRCPPLAY
                "AVRCPPAUSE",    //BT_REQUEST_AVRCPPAUSE
                "AVRCPSTOP",     //BT_REQUEST_AVRCPSTOP
                "AVRCPFORWARD",  //BT_REQUEST_AVRCPFORWARD
                "AVRCPBACKWARD", //BT_REQUEST_AVRCPBACKWARD
                "AVRCPVOLUMEUP", //BT_REQUEST_AVRCPVOLUMEUP
                "AVRCPVOLUMEDOWN", //BT_REQUEST_AVRCPVOLUMEDOWN
                "AVRCPSABSVOL",    //BT_REQUEST_AVRCPSABSVOL
                "PBCCONN",         //BT_REQUEST_PBCCONN
                "PBCDISC",         //BT_REQUEST_PBCDISC
                "PBCPULLPB",       //BT_REQUEST_PBCPULLPB
                "PBCPULLCONT",     //BT_REQUEST_PBCPULLCONT
                "PBCPULLCRT",      //BT_REQUEST_PBCPULLCRT
                "PBCPULLCMT",      //BT_REQUEST_PBCPULLCMT
                "PBCSETPARSE",     //BT_REQUEST_PBCSETPARSE
                "PBCGETPARSE",     //BT_REQUEST_PBCGETPARSE
                "HIDCONN",         //BT_REQUEST_HIDCONN
                "HIDDISC",         //BT_REQUEST_HIDDISC
                "MAPCCONN",        //BT_REQUEST_MAPCCONN
                "MAPCDIS",         //BT_REQUEST_MAPCDIS
                "MAPCGETML",       //BT_REQUEST_MAPCGETML
                "MAPCGETCONT",     //BT_REQUEST_MAPCGETCONT
                "MAPCGETMSG",      //BT_REQUEST_MAPCGETMSG
                "MAPCGETCRT",      // BT_REQUEST_MAPCGETCRT
                "MAPCPUSHMSG",     //BT_REQUEST_MAPCPUSHMSG
                "MAPCCMT",         //BT_REQUEST_MAPCCMT
                "GPRL",            //BT_REQUEST_GPRL
        };
    }

    //GEN
    static final int BT_UNSOL_RESPONSE_INIT = 1;
    static final int BT_UNSOL_RESPONSE_ROLE = BT_UNSOL_RESPONSE_INIT + 1;
    static final int BT_UNSOL_RESPONSE_PAIR = BT_UNSOL_RESPONSE_ROLE + 1;
    static final int BT_UNSOL_RESPONSE_INQR = BT_UNSOL_RESPONSE_PAIR + 1;
    static final int BT_UNSOL_RESPONSE_INQC = BT_UNSOL_RESPONSE_INQR + 1;

    //HFP
    static final int BT_UNSOL_RESPONSE_HFSTAT = BT_UNSOL_RESPONSE_INQC + 1;
    static final int BT_UNSOL_RESPONSE_HFCONN = BT_UNSOL_RESPONSE_HFSTAT + 1;
    static final int BT_UNSOL_RESPONSE_HFDISC = BT_UNSOL_RESPONSE_HFCONN + 1;
    static final int BT_UNSOL_RESPONSE_HFRING = BT_UNSOL_RESPONSE_HFDISC + 1;
    static final int BT_UNSOL_RESPONSE_HFIBRN = BT_UNSOL_RESPONSE_HFRING + 1;
    static final int BT_UNSOL_RESPONSE_HFAUDIO = BT_UNSOL_RESPONSE_HFIBRN + 1;
    static final int BT_UNSOL_RESPONSE_HFCLIP = BT_UNSOL_RESPONSE_HFAUDIO + 1;
    static final int BT_UNSOL_RESPONSE_HFCCWA = BT_UNSOL_RESPONSE_HFCLIP + 1;
    static final int BT_UNSOL_RESPONSE_HFNUML = BT_UNSOL_RESPONSE_HFCCWA + 1;
    static final int BT_UNSOL_RESPONSE_HFNUMC = BT_UNSOL_RESPONSE_HFNUML + 1;
    static final int BT_UNSOL_RESPONSE_HFSGNL = BT_UNSOL_RESPONSE_HFNUMC + 1;
    static final int BT_UNSOL_RESPONSE_HFROAM = BT_UNSOL_RESPONSE_HFSGNL + 1;
    static final int BT_UNSOL_RESPONSE_HFBATC = BT_UNSOL_RESPONSE_HFROAM + 1;
    static final int BT_UNSOL_RESPONSE_HFVGSI = BT_UNSOL_RESPONSE_HFBATC + 1;
    static final int BT_UNSOL_RESPONSE_HFVGMI = BT_UNSOL_RESPONSE_HFVGSI + 1;
    static final int BT_UNSOL_RESPONSE_HFSRVC = BT_UNSOL_RESPONSE_HFVGMI + 1;
    static final int BT_UNSOL_RESPONSE_HFCHLD = BT_UNSOL_RESPONSE_HFSRVC + 1;
    static final int BT_UNSOL_RESPONSE_HFCCIN = BT_UNSOL_RESPONSE_HFCHLD + 1;

    //A2DP
    static final int BT_UNSOL_RESPONSE_A2DPSTAT = BT_UNSOL_RESPONSE_HFCCIN + 1;
    static final int BT_UNSOL_RESPONSE_A2DPCONN = BT_UNSOL_RESPONSE_A2DPSTAT + 1;
    static final int BT_UNSOL_RESPONSE_A2DPAUDIO = BT_UNSOL_RESPONSE_A2DPCONN + 1;

    //AVRCP
    static final int BT_UNSOL_RESPONSE_AVRCPSTAT = BT_UNSOL_RESPONSE_A2DPAUDIO + 1;
    static final int BT_UNSOL_RESPONSE_AVRCPCONN = BT_UNSOL_RESPONSE_AVRCPSTAT + 1;
    static final int BT_UNSOL_RESPONSE_AVRCPDISC = BT_UNSOL_RESPONSE_AVRCPCONN + 1;
    static final int BT_UNSOL_RESPONSE_AVRCPTITLE = BT_UNSOL_RESPONSE_AVRCPDISC + 1;
    static final int BT_UNSOL_RESPONSE_AVRCPARTIST = BT_UNSOL_RESPONSE_AVRCPTITLE + 1;
    static final int BT_UNSOL_RESPONSE_PLAYSTATUS = BT_UNSOL_RESPONSE_AVRCPARTIST + 1;
    static final int BT_UNSOL_RESPONSE_AVRCPFEATURE = BT_UNSOL_RESPONSE_PLAYSTATUS + 1;

    //PBAP
    static final int BT_UNSOL_RESPONSE_PBCSTAT = BT_UNSOL_RESPONSE_AVRCPFEATURE + 1;
    static final int BT_UNSOL_RESPONSE_PBCCONN = BT_UNSOL_RESPONSE_PBCSTAT + 1;
    static final int BT_UNSOL_RESPONSE_PBPULLDATAIND = BT_UNSOL_RESPONSE_PBCCONN + 1;
    static final int BT_UNSOL_RESPONSE_PBCPULLCMTIND = BT_UNSOL_RESPONSE_PBPULLDATAIND + 1;
    static final int BT_UNSOL_RESPONSE_PBCPARSEDATAIND = BT_UNSOL_RESPONSE_PBCPULLCMTIND + 1;
    static final int BT_UNSOL_RESPONSE_PBCPULLPB = BT_UNSOL_RESPONSE_PBCPARSEDATAIND + 1;
    static final int BT_UNSOL_RESPONSE_PBCPULLCONT = BT_UNSOL_RESPONSE_PBCPULLPB + 1;
    static final int BT_UNSOL_RESPONSE_PBCPULLCRT = BT_UNSOL_RESPONSE_PBCPULLCONT + 1;
    static final int BT_UNSOL_RESPONSE_PBCPULLCMT = BT_UNSOL_RESPONSE_PBCPULLCRT + 1;

    //OTHER
    static final int BT_UNSOL_RESPONSE_GRDN = BT_UNSOL_RESPONSE_PBCPULLCMT + 1;

    //MAP
    static final int BT_UNSOL_RESPONSE_MAPCINIT = BT_UNSOL_RESPONSE_GRDN + 1;
    static final int BT_UNSOL_RESPONSE_MAPCCONN = BT_UNSOL_RESPONSE_MAPCINIT + 1;
    static final int BT_UNSOL_RESPONSE_MAPCDISC = BT_UNSOL_RESPONSE_MAPCCONN + 1;
    static final int BT_UNSOL_RESPONSE_MAPCGETDATAIND = BT_UNSOL_RESPONSE_MAPCDISC + 1;
    static final int BT_UNSOL_RESPONSE_MAPCGETCMTIND = BT_UNSOL_RESPONSE_MAPCGETDATAIND + 1;
    static final int BT_UNSOL_RESPONSE_MAPCPUSHCONTIND = BT_UNSOL_RESPONSE_MAPCGETCMTIND + 1;
    static final int BT_UNSOL_RESPONSE_MAPCPUSHCMTIND = BT_UNSOL_RESPONSE_MAPCPUSHCONTIND + 1;
    static final int BT_UNSOL_RESPONSE_MAPCEVTIND = BT_UNSOL_RESPONSE_MAPCPUSHCMTIND + 1;

    //OTHER
    static final int BT_UNSOL_RESPONSE_GLDN = BT_UNSOL_RESPONSE_MAPCEVTIND + 1;
    static final int BT_UNSOL_RESPONSE_MUTE = BT_UNSOL_RESPONSE_GLDN + 1;
    static final int BT_UNSOL_RESPONSE_GPRL = BT_UNSOL_RESPONSE_MUTE + 1;
    static final int BT_UNSOL_RESPONSE_GLBD = BT_UNSOL_RESPONSE_GPRL + 1;
    static final int BT_UNSOL_RESPONSE_RING = BT_UNSOL_RESPONSE_GLBD + 1;
    static final int BT_UNSOL_RESPONSE_AVRCPPLAY = BT_UNSOL_RESPONSE_RING + 1;
    static final int BT_UNSOL_RESPONSE_AVRCPPAUSE = BT_UNSOL_RESPONSE_AVRCPPLAY + 1;

    //SPP
    static final int BT_UNSOL_RESPONSE_SPPSTAT = 57;
    static final int BT_UNSOL_RESPONSE_SPPCONN = 58;
    static final int BT_UNSOL_RESPONSE_SPPDISC = 59;
    static final int BT_UNSOL_RESPONSE_SPPDATAIND = 60;

    static HashMap<String, Integer> eventMap = new HashMap<String, Integer>();

    static
    {
        eventMap.put("SNKINIT", BT_UNSOL_RESPONSE_INIT);
        eventMap.put("ROLE", BT_UNSOL_RESPONSE_ROLE);
        eventMap.put("PAIR", BT_UNSOL_RESPONSE_PAIR);
        eventMap.put("INQR", BT_UNSOL_RESPONSE_INQR);
        eventMap.put("INQC", BT_UNSOL_RESPONSE_INQC);

        eventMap.put("HFSTAT", BT_UNSOL_RESPONSE_HFSTAT);
        eventMap.put("HFCONN", BT_UNSOL_RESPONSE_HFCONN);
        eventMap.put("HFDISC", BT_UNSOL_RESPONSE_HFDISC);
        eventMap.put("HFRING", BT_UNSOL_RESPONSE_HFRING);
        eventMap.put("HFIBRN", BT_UNSOL_RESPONSE_HFIBRN);
        eventMap.put("HFAUDIO", BT_UNSOL_RESPONSE_HFAUDIO);
        eventMap.put("HFCIP", BT_UNSOL_RESPONSE_HFCLIP);
        eventMap.put("HFCCWA", BT_UNSOL_RESPONSE_HFCCWA);
        eventMap.put("HFNUML", BT_UNSOL_RESPONSE_HFNUML);
        eventMap.put("HFNUMC", BT_UNSOL_RESPONSE_HFNUMC);
        eventMap.put("HFSGNL", BT_UNSOL_RESPONSE_HFSGNL);
        eventMap.put("HFROAM", BT_UNSOL_RESPONSE_HFROAM);
        eventMap.put("HFBATC", BT_UNSOL_RESPONSE_HFBATC);
        eventMap.put("HFVGSI", BT_UNSOL_RESPONSE_HFVGSI);
        eventMap.put("HFVGMI", BT_UNSOL_RESPONSE_HFVGMI);
        eventMap.put("HFSRVC", BT_UNSOL_RESPONSE_HFSRVC);
        eventMap.put("HFCHLD", BT_UNSOL_RESPONSE_HFCHLD);
        eventMap.put("HFCCIN", BT_UNSOL_RESPONSE_HFCCIN);

        eventMap.put("A2DPSTAT", BT_UNSOL_RESPONSE_A2DPSTAT);
        eventMap.put("A2DPCONN", BT_UNSOL_RESPONSE_A2DPCONN);
        eventMap.put("A2DPAUDIO", BT_UNSOL_RESPONSE_A2DPAUDIO);

        eventMap.put("AVRCPSTAT", BT_UNSOL_RESPONSE_AVRCPSTAT);
        eventMap.put("AVRCPCONN", BT_UNSOL_RESPONSE_AVRCPCONN);
        eventMap.put("AVRCPDISC", BT_UNSOL_RESPONSE_AVRCPDISC);
        eventMap.put("AVRCPTITLE", BT_UNSOL_RESPONSE_AVRCPTITLE);
        eventMap.put("AVRCPARTIST", BT_UNSOL_RESPONSE_AVRCPARTIST);
        eventMap.put("PLAYSTATUS", BT_UNSOL_RESPONSE_PLAYSTATUS);
        eventMap.put("AVRCPFEATURE", BT_UNSOL_RESPONSE_AVRCPFEATURE);

        eventMap.put("PBCSTAT", BT_UNSOL_RESPONSE_PBCSTAT);
        eventMap.put("PBCCONN", BT_UNSOL_RESPONSE_PBCCONN);
        eventMap.put("PBPULLDATAIND", BT_UNSOL_RESPONSE_PBPULLDATAIND);
        eventMap.put("PBCPULLCMTIND", BT_UNSOL_RESPONSE_PBCPULLCMTIND);
        eventMap.put("PBCPARSEDATAIND", BT_UNSOL_RESPONSE_PBCPARSEDATAIND);
        eventMap.put("PBCPULLPB", BT_UNSOL_RESPONSE_PBCPULLPB);
        eventMap.put("PBCPULLCONT", BT_UNSOL_RESPONSE_PBCPULLCONT);
        eventMap.put("PBCPULLCRT", BT_UNSOL_RESPONSE_PBCPULLCRT);
        eventMap.put("PBCPULLCMT", BT_UNSOL_RESPONSE_PBCPULLCMT);
        eventMap.put("GRDN", BT_UNSOL_RESPONSE_GRDN);
        eventMap.put("MAPCINIT", BT_UNSOL_RESPONSE_MAPCINIT);
        eventMap.put("MAPCCONN", BT_UNSOL_RESPONSE_MAPCCONN);
        eventMap.put("MAPCDISC", BT_UNSOL_RESPONSE_MAPCDISC);
        eventMap.put("MAPCGETDATAIND", BT_UNSOL_RESPONSE_MAPCGETDATAIND);
        eventMap.put("MAPCGETCMTIND", BT_UNSOL_RESPONSE_MAPCGETCMTIND);
        eventMap.put("MAPCPUSHCONTIND", BT_UNSOL_RESPONSE_MAPCPUSHCONTIND);
        eventMap.put("MAPCPUSHCMTIND", BT_UNSOL_RESPONSE_MAPCPUSHCMTIND);
        eventMap.put("MAPCEVTIND", BT_UNSOL_RESPONSE_MAPCEVTIND);
        eventMap.put("GLDN", BT_UNSOL_RESPONSE_GLDN);
        eventMap.put("MUTE", BT_UNSOL_RESPONSE_MUTE);
        eventMap.put("GPRL", BT_UNSOL_RESPONSE_GPRL);
        eventMap.put("GLBD", BT_UNSOL_RESPONSE_GLBD);
        eventMap.put("RING", BT_UNSOL_RESPONSE_RING);
        eventMap.put("AVRCPPLAY", BT_UNSOL_RESPONSE_AVRCPPLAY);
        eventMap.put("AVRCPPAUSE", BT_UNSOL_RESPONSE_AVRCPPAUSE);
        eventMap.put("INQR", BT_UNSOL_RESPONSE_INQR);
    }

    static int getEventCode(String key)
    {
        if (eventMap.containsKey(key))
        {
            return eventMap.get(key);
        }
        return -1;
    }

    static String[] getEventCommands()
    {
        return new String[]{"SNKINIT",   //BT_UNSOL_RESPONSE_INIT
                "ROLE",      //BT_UNSOL_RESPONSE_ROLE
                "PAIR",      //BT_UNSOL_RESPONSE_PAIR
                "HFSTAT",    //BT_UNSOL_RESPONSE_HFSTAT
                "HFCONN",    //BT_UNSOL_RESPONSE_HFCONN
                "HFDISC",    //BT_UNSOL_RESPONSE_HFDISC
                "HFRING",    //BT_UNSOL_RESPONSE_HFRING
                "HFIBRN",    //BT_UNSOL_RESPONSE_HFIBRN
                "HFAUDIO",   //BT_UNSOL_RESPONSE_HFAUDIO
                "HFCIP",     //BT_UNSOL_RESPONSE_HFCLIP
                "HFCCWA",    //BT_UNSOL_RESPONSE_HFCCWA
                "HFNUML",    //BT_UNSOL_RESPONSE_HFNUML
                "HFNUMC",    //BT_UNSOL_RESPONSE_HFNUMC
                "HFSGNL",    //BT_UNSOL_RESPONSE_HFSGNL
                "HFROAM",    //BT_UNSOL_RESPONSE_HFROAM
                "HFBATC",    //BT_UNSOL_RESPONSE_HFBATC
                "HFVGSI",    //BT_UNSOL_RESPONSE_HFVGSI
                "HFVGMI",    //BT_UNSOL_RESPONSE_HFVGMI
                "HFSRVC",    //BT_UNSOL_RESPONSE_HFSRVC
                "HFCHLD",    //BT_UNSOL_RESPONSE_HFCHLD
                "HFCCIN",    //BT_UNSOL_RESPONSE_HFCCIN
                "A2DPSTAT",  //BT_UNSOL_RESPONSE_A2DPSTAT
                "A2DPCONN",  //BT_UNSOL_RESPONSE_A2DPCONN
                "A2DPAUDIO", //BT_UNSOL_RESPONSE_A2DPAUDIO
                "AVRCPSTAT",        //BT_UNSOL_RESPONSE_AVRCPSTAT
                "AVRCPCONN",        //BT_UNSOL_RESPONSE_AVRCPCONN
                "AVRCPDISC",        //BT_UNSOL_RESPONSE_AVRCPDISC
                "AVRCPTITLE",       //BT_UNSOL_RESPONSE_AVRCPTITLE
                "AVRCPARTIST",      //BT_UNSOL_RESPONSE_AVRCPARTIST
                "PLAYSTATUS",       //BT_UNSOL_RESPONSE_PLAYSTATUS
                "AVRCPFEATURE",     //BT_UNSOL_RESPONSE_AVRCPFEATURE
                "PBCSTAT",          //BT_UNSOL_RESPONSE_PBCSTAT
                "PBCCONN",          //BT_UNSOL_RESPONSE_PBCCONN
                "PBPULLDATAIND",    //BT_UNSOL_RESPONSE_PBPULLDATAIND
                "PBCPULLCMTIND",    //BT_UNSOL_RESPONSE_PBCPULLCMTIND
                "PBCPARSEDATAIND",  //BT_UNSOL_RESPONSE_PBCPARSEDATAIND
                "PBCPULLPB",        //BT_UNSOL_RESPONSE_PBCPULLPB
                "PBCPULLCONT",      //BT_UNSOL_RESPONSE_PBCPULLCONT
                "PBCPULLCRT",       //BT_UNSOL_RESPONSE_PBCPULLCRT
                "PBCPULLCMT",       //BT_UNSOL_RESPONSE_PBCPULLCMT
                "GRDN",             //BT_UNSOL_RESPONSE_GRDN
                "MAPCINIT",         //BT_UNSOL_RESPONSE_MAPCINIT
                "MAPCCONN",         //BT_UNSOL_RESPONSE_MAPCCONN
                "MAPCDISC",         //BT_UNSOL_RESPONSE_MAPCDISC
                "MAPCGETDATAIND",   //BT_UNSOL_RESPONSE_MAPCGETDATAIND
                "MAPCGETCMTIND",    //BT_UNSOL_RESPONSE_MAPCGETCMTIND
                "MAPCPUSHCONTIND",  //BT_UNSOL_RESPONSE_MAPCPUSHCONTIND
                "MAPCPUSHCMTIND",   //BT_UNSOL_RESPONSE_MAPCPUSHCMTIND
                "MAPCEVTIND",       //BT_UNSOL_RESPONSE_MAPCEVTIND
                "GLDN",             //BT_UNSOL_RESPONSE_GLDN
                "MUTE",             //BT_UNSOL_RESPONSE_MUTE
                "GPRL",             //BT_UNSOL_RESPONSE_GPRL
                "GLBD",             //BT_UNSOL_RESPONSE_GLBD
                "RING",             //BT_UNSOL_RESPONSE_RING
                "AVRCPPLAY",        //BT_UNSOL_RESPONSE_AVRCPPLAY
                "AVRCPPAUSE",       //BT_UNSOL_RESPONSE_AVRCPPAUSE
        };
    }

    public static String requestToString(int requestId)
    {
        switch (requestId)
        {
            case BTConstants.BT_REQUEST_SEND_CMD:
                return "SEND COMMAND";
            case BTConstants.BT_REQUEST_GVER:
                return "GVER";
            case BTConstants.BT_REQUEST_GLBD:
                return "GLBD";
            case BTConstants.BT_REQUEST_GLDN:
                return "GLDN";
            case BTConstants.BT_REQUEST_SLDN:
                return "SLDN";
            case BTConstants.BT_REQUEST_GRDN:
                return "GRDN";
            case BTConstants.BT_REQUEST_SPIN:
                return "SPIN";
            case BTConstants.BT_REQUEST_GPIN:
                return "GPIN";
            case BTConstants.BT_REQUEST_GPRD:
                return "GPRD";
            case BTConstants.BT_REQUEST_DPRD:
                return "DPRD";
            case BTConstants.BT_REQUEST_INQU:
                return "INQU";
            case BTConstants.BT_REQUEST_PAIR:
                return "PAIR";
            case BTConstants.BT_REQUEST_SCAN:
                return "SCAN";
            case BTConstants.BT_REQUEST_EDFU:
                return "EDFU";
            case BTConstants.BT_REQUEST_UART:
                return "UART";
            case BTConstants.BT_REQUEST_SCOD:
                return "SCOD";
            case BTConstants.BT_REQUEST_GCOD:
                return "GCOD";
            case BTConstants.BT_REQUEST_SPRO:
                return "SPRO";
            case BTConstants.BT_REQUEST_HFCONN:
                return "HFCONN";
            case BTConstants.BT_REQUEST_HFDISC:
                return "HFDISC";
            case BTConstants.BT_REQUEST_HFANSW:
                return "HFANSW";
            case BTConstants.BT_REQUEST_HFCHUP:
                return "HFCHUP";
            case BTConstants.BT_REQUEST_HFDIAL:
                return "HFDIAL";
            case BTConstants.BT_REQUEST_HFDTMF:
                return "HFDTMF";
            case BTConstants.BT_REQUEST_HFCTRS:
                return "HFCTRS";
            case BTConstants.BT_REQUEST_HFMCAL:
                return "HFMCAL";
            case BTConstants.BT_REQUEST_HFCLCC:
                return "HFCLCC";
            case BTConstants.BT_REQUEST_HFSVGS:
                return "HFSVGS";
            case BTConstants.BT_REQUEST_HFGVGS:
                return "HFGVGS";
            case BTConstants.BT_REQUEST_HFSVGM:
                return "HFSVGM";
            case BTConstants.BT_REQUEST_HFGVGM:
                return "HFGVGM";
            case BTConstants.BT_REQUEST_HFMUTE:
                return "HFMUTE";
            case BTConstants.BT_REQUEST_HFSCFG:
                return "HFSCFG";
            case BTConstants.BT_REQUEST_HFGCFG:
                return "HFGCFG";
        }
        return "<unknown request>";
    }

    static public String responseToString(int responseId)
    {
        if (eventMap.containsValue(responseId))
        {
        }
        switch (responseId)
        {
            //	case BTConstants.BT_UNSOL_RESPONSE_SOURCE_NOTIFICATION: return "SOURCE_NOTIFICATION";

        }
        return "<unknown reponse>" + responseId;
    }

    //Profile Type
    public static final int BT_HFP = 0;
    public static final int BT_A2DP = 1;
    public static final int BT_AVRCP = 2;
    public static final int BT_PBAP = 3;
    public static final int BT_HID = 4;
    public static final int BT_MAP = 5;


    //Constants for initiating pairing .
    static final int BT_PAIRING_INITIATE = 0;
    static final int BT_PAIRING_ACCEPT = 1;
    static final int BT_PAIRING_REJECT = 2;

    //Bond State notification
    static final int BT_PAIRING_STATUS_IN_PROCESS = -1;
    static final int BT_PAIRING_STATUS_FAILED = 0;
    static final int BT_PAIRING_STATUS_SUCCEEDED = 1;
    static final int BT_PAIRING_STATUS_BAD_PIN = 2;
    static final int BT_PAIRING_STATUS_NOT_FOUND = 3;
    static final int BT_PAIRING_STATUS_MAX_DEV_REACHED = 4;

    static final int BT_PAIRING_IO_CAPABILITY_DISPLAY_ONLY = 0;
    static final int BT_PAIRING_IO_CAPABILITY_DISPLAY_YES_NO = 1;
    static final int BT_PAIRING_IO_CAPABILITY_KEYBOARD_ONLY = 2;
    static final int BT_PAIRING_IO_CAPABILITY_NO_IO = 3;

    static final int BT_PAIRING_SSP_MODE_NUMERIC_COMPARISION = 0;
    static final int BT_PAIRING_SSP_MODE_NUMERIC_PASSKEY_INPUT = 1;
    static final int BT_PAIRING_SSP_MODE_NUMERIC_PASSKEY_DISPLAY = 2;

    static final int BT_PAIRING_MODE_REFUSE = 0;
    static final int BT_PAIRING_MODE_FORWARD = 1;
    static final int BT_PAIRING_MODE_ACCEPT = 2;
    static final int BT_PAIRING_MODE_FORWARD_EXTENDED_DEVICE = 3;
    static final int BT_PAIRING_MODE_FORWARD_EXTENDED_DEVICE_ADDRESS = 4;

    static final int BT_ACTION_PLAYER_STOP = 0;
    static final int BT_ACTION_PLAYER_PAUSE = 1;
    static final int BT_ACTION_PLAYER_RESUME = 2;
    static final int BT_ACTION_PLAYER_NEXT = 3;
    static final int BT_ACTION_PLAYER_PREVIOUS = 4;
    static final int BT_ACTION_PLAYER_FAST_FORWARD = 5;
    static final int BT_ACTION_PLAYER_FAST_REVIND = 6;
    static final int BT_ACTION_PLAYER_SEEK_TO = 7;
    static final int BT_ACTION_PLAYER_TOGGLE_PAUSE_RESUME = 8;
    static final int BT_ACTION_PLAYER_FAST_FORWARD_REWIND = 9;

    static final int BT_CALL_STATE_FREE = -1;
    static final int BT_CALL_STATE_ACTIVE = 0;
    static final int BT_CALL_STATE_HELD = 1;
    static final int BT_CALL_STATE_DAILING = 2;
    static final int BT_CALL_STATE_RINGING = 3;
    static final int BT_CALL_STATE_INCOMING = 4;
    static final int BT_CALL_STATE_WAITING = 5;
    static final int BT_CALL_STATE_RELEASED = 6;

    static final int BT_DEVICE_STATE_CONNECT_OK = 0;
    static final int BT_DEVICE_STATE_CONNECT_FAILED = 1;
    static final int BT_DEVICE_STATE_CONNECT_NOT_FOUND = 2;
    static final int BT_DEVICE_STATE_DISCONNECT_OK = 3;
    static final int BT_DEVICE_STATE_DISCONNECT_FAILED = 4;
    static final int BT_DEVICE_STATE_AUTOCONNECT = 5;
    static final int BT_DEVICE_STATE_CONNECT_ABORT = 6;
    static final int BT_DEVICE_STATE_CONNECT_DEFAULT_SERVICE = 7;
    static final int BT_DEVICE_STATE_USER_ALLREADY_STARTING = 8;

    //	static final int BT_PLAYER_STATUS_STOP = 0;
    //	static final int BT_PLAYER_STATUS_PAUSE= 1;
    //	static final int BT_PLAYER_STATUS_PLAY = 2;
    //	static final int BT_PLAYER_STATUS_MUTED = 3;
    //	static final int BT_PLAYER_STATUS_FSTFWD = 4;
    //	static final int BT_PLAYER_STATUS_REWIND = 5;
    //	static final int BT_PLAYER_STATUS_DISCONNECTED = 6;
    //	static final int BT_PLAYER_STATUS_CONNECTING=7;
    //	static final int BT_PLAYER_STATUS_CONNECTED = 8;
    //	static final int BT_PLAYER_STATUS_STREAMING = 9;
    //	static final int BT_PLAYER_STATUS_UNKNOWN = 10;

    //RepeateMode
    static final int PALYER_MODE_REPEATE_CURRENT = 2;
    static final int PALYER_MODE_REPEATE_ALL = 3;

    //////////////////////////////////////////////////
    //HFP Status
    public static final int BT_STATE_HFP_READY = 1;
    public static final int BT_STATE_HFP_CONNECTING = 2;
    public static final int BT_STATE_HFP_CONNECTED = 3;
    public static final int BT_STATE_HFP_INCOMMINGCALL_ESTABLISH = 4;
    public static final int BT_STATE_HFP_OUTGOINGCALL_ESTABLISH = 5;
    public static final int BT_STATE_HFP_ACTIVECALL = 6;
    public static final int BT_STATE_HFP_WCALLING = 7;


    //MUSIC Play Status for IVT
    static final int IVT_PLAY_STATUS_STOP = 0x00;
    static final int IVT_PLAY_STATUS_PLAYING = 0x01;
    static final int IVT_PLAY_STATUS_PAUSED = 0x02;
    static final int IVT_PLAY_STATUS_FSTFWD = 0x03;
    static final int IVT_PLAY_STATUS_REWIND = 0x04;
    static final int IVT_PLAY_STATUS_ERROR = 0xFF;

    // MUSIC A2DP STATE for IVT
    static final int IVT_A2DP_STATE_READY = 1;
    static final int IVT_A2DP_STATE_CONNECTING = 2;
    static final int IVT_A2DP_STATE_CONNTECTED = 3;
    static final int IVT_A2DP_STATE_STREAM = 4;

    static final int BT_ACTION_MULTICALL_UDUB = 0;  //sets user determinated user buys for a waiting call
    static final int BT_ACTION_MULTICALL_RAAH = 1;  //release all active call and accept other held or waiting call
    static final int BT_ACTION_MULTICALL_PAAH = 2;  //place all active calls on hold and accepts the other call
    static final int BT_ACTION_MULTICALL_ADCO = 3;  //add a held call to conversation
    static final int BT_ACTION_MULTICALL_CATR = 4;  //call transfer
}
