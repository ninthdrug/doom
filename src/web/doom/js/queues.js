var DOMAINS = new Array();
var SERVERS = new Array();
var DOMAINLIST = [];
var SERVERLIST = [];
var PERMISSIONS = [];
var DOMAINGROUPS = [];
var USERID = readCookie('userid');
var MYGROUPS = getMyGroups();

function popup(obj) {
    var servername = $('#servername').attr('value');
    var queuename = $(obj).parent().parent().attr('id');
    $('#' + queuename + ' td:gt(0)').text('');
    $('#backup_server').text(servername);
    $('#backup_queue').text(queuename);
    $('#backup_status').text('working...');
    $('#backuppopup').dialog('open');
    $.get('backupqueue.taak',
           { "server" : servername, "queue" : queuename },
           function(data) {
               $('#backup_status').text(data)
               $.getJSON("queuestates.json", { "server" : servername }, updateQueueStates);
           },
           "html");
}

function canBackupQueue() {
    return index("admins", MYGROUPS) != -1 ||
           index("gods", MYGROUPS) != -1;
}

function updateQueueStates(queues) {
    var text = [];
    var odd = 0;

    for (var i in queues) {
        var queue = queues[i];
        var name = queue.name;
        var jndi = queue.jndi;
        var messageCount = queue.messageCount;
        var pendingMessageCount = queue.pendingMessageCount;
        var link = ""
        if (canBackupQueue() && messageCount != "0") {
            link = "<a href='#' onclick='javascript:return popup(this)'>backup</a>"
        }
        var row = "<tr " + (odd ? " class='odd' " : "") +
                          "id='" + jndi + "'>" +
                  "<td>" + name + "</td>" +
                  "<td>" + messageCount + "</td>" +
                  "<td>" + pendingMessageCount + "</td>" +
                  "<td>" + link + "</td>" +
                  "</tr>"
        text.unshift(row);
        ood = 1 - odd;
    }
    $("#queues_table tbody").html(text.join(''));
}

function initConfig(config) {
    PERMISSIONS = config.permissions;
    DOMAINLIST = config.domains;
    SERVERLIST = config.servers;
    for (var i in DOMAINLIST) {
        var domain = DOMAINLIST[i];
        domain.adminserver = null;
        domain.managedservers = [];
        domain.nodemanagers = [];
        DOMAINS[domain.name] = domain;
        var group = domain.group;
        if (index(group, DOMAINGROUPS) == -1) {
            DOMAINGROUPS.push(group);
        }
    }
    for (var i in SERVERLIST) {
        var server = SERVERLIST[i];
        var domain = DOMAINS[server.domainname];
        if (domain) {
            if (server.servertype == "admin") {
                domain.adminserver = server;
            } else if (server.servertype == "managed") {
                domain.managedservers.push(server);
            } else if (server.servertype == "nodemanager") {
                domain.nodemanagers.push(server);
            } else if (server.servertype == "loadbalancer") {
                domain.loadbalancer = server;
            }

            server.domain = domain;
        }
        SERVERS[server.servername] = server;
    }
}

function getMyGroups() {
    var mygroups = readCookie('mygroups');
    if (mygroups != null) {
        return mygroups.split(" ");
    }
    return [];
}

function readCookie(name) {
    var nameEQ = name + "=";
    var ca = document.cookie.split(';');
    for (var i = 0; i < ca.length; i++) {
        var c = ca[i];
        while (c.charAt(0) == ' ') c = c.substring(1, c.length);
        if (c.indexOf(nameEQ) == 0) {
            return c.substring(nameEQ.length, c.length);
        }
    }
    return null;
}

function index(item, array) {
    for (var i in array) {
        if (array[i] == item) return i;
    }
    return -1;
}

$(document).ready(function() {
    $.ajaxSetup({ cache: false });

    $('#errorpopup').dialog({
        title: 'Error',
        autoOpen: false,
        width: 340
    });

    $('#backuppopup').dialog({
        title: 'backup queue',
        autoOpen: false,
        width: 340
    });

    $("#tree_div").jstree({
        "json_data" : {
            "ajax" : {
                "url" : "serverstree.json",
                "data" : function(n) {
                    return { id : n.attr ? n.attr("id") : 0 };
                }
            }
        },
        "ui" : {
            "select_limit" : 1
        },
        "plugins" : [ "themes", "json_data", "ui" ]
    }).bind("select_node.jstree", function(event, data) {
        var id = data.rslt.obj.attr("id");
        $('#servername').attr('value', id);
        $.getJSON("queuestates.json", { "server" : id }, updateQueueStates);
    });

    $.getJSON("config.json", {}, initConfig);
});
