var DOMAINS = new Array();
var SERVERS = new Array();
var USERS = [];
var DOMAINLIST = [];
var SERVERLIST = [];
var PERMISSIONS = [];
var DOMAINGROUPS = [];
var USERID = readCookie('userid');
var MYGROUPS = getMyGroups();
var HEALTH2CLASS = {
    "ADMIN" : "critical",
    "BLOCKED" : "warn",
    "CRITICAL" : "critical",
    "DOWN" : "critical",
    "DYING" : "critical",
    "FAILED" : "critical",
    "OK" : "ok",
    "OVERLOADED" : "critical",
    "RESUMING" : "warn",
    "RUNNING" : "warn",
    "STANDBY" : "warn",
    "STARTING" : "warn",
    "STOPPING" : "warn",
    "SUSPENDING" : "warn",
    "WARN" : "warn"
};

function debug(msg) {
    $("#debug").append(msg + "<br/>");
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

function finddomain(group, env) {
    for (var i in DOMAINLIST) {
        var domain = DOMAINLIST[i];
        if (domain.group == group && domain.env == env) {
            return domain;
        }
    }
    return null;
}

function canControl(domain) {
    for (var i in PERMISSIONS) {
        var p = PERMISSIONS[i];
        if ((p.project == '' || p.project == domain.project) &&
            (p.env == '' || p.env == domain.env) &&
            (p.groupname == '' || index(p.groupname, MYGROUPS) != -1) &&
            (p.action == '' || p.action == 'bea_operator')
        ) {
            return true;
        }
    }
    return false;
}

function formatCell(server) {
    var cell = "<td></td>";
    if (server != null) {
        var id = server.servername;
        var link = '<a href="#" onclick="javascript:return popup(this)">?</a>';
        var dis = canControl(server.domain) ? '' : ' disabled="disabled" ';
        cell = '<td id="' + id + '">' +
                 '<input type="checkbox" name="' + id + '"' + dis +'/>' +
                  link +
               '</td>';
    }
    return cell;
}

function initTable(config) {
    PERMISSIONS = config.permissions;
    DOMAINLIST = config.domains;
    SERVERLIST = config.servers;
    USERS = config.users;
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
    var envs = [ "dev", "tst", "acc", "prd" ];
    var text = [];
    var n = 0;
    for (var i in DOMAINGROUPS) {
        var domaingroup = DOMAINGROUPS[i];
        text[n++] = '<tr><td class="domaingroup">' + domaingroup + '</td>';
        for (var e in envs) {
            var env =  envs[e];
            var domain = finddomain(domaingroup, env);
            if (domain == null) {
                text[n++] = "<td></td><td></td><td></td><td></td><td></td><td></td>";
            } else {
                var lb = domain.loadbalancer;
                var nms = domain.nodemanagers;
                var ms = domain.managedservers;
                var nm1 = nms.length < 1 ? null : nms[0];
                var nm2 = nms.length < 2 ? null : nms[1];
                var admin = domain.adminserver;
                var ms1 = ms.length < 1 ? null : ms[0];
                var ms2 = ms.length < 2 ? null : ms[1];
                text[n++] = lb == null ? '<td></td>' : formatCell(lb)
                text[n++] = formatCell(nm1);
                text[n++] = formatCell(nm2);
                text[n++] = formatCell(admin);
                text[n++] = formatCell(ms1);
                text[n++] = formatCell(ms2);
            }
        }
        text[n++] = '</tr>\n';
    }
    var tbody = $('#servers_table tbody');
    tbody.html(text.join(''));
    $('#servers_table tbody tr').hover(
        function() { $(this).children().addClass("highlight"); },
        function() { $(this).children().removeClass("highlight"); }
    );
    tbody.find('td:nth-child(8)').addClass('sep');
    tbody.find('td:nth-child(14)').addClass('sep');
    tbody.find('td:nth-child(20)').addClass('sep');

    text = [];
    n = 0;
    text[n++] = '<option value=""></option>';
    for (var i in USERS) {
        var user = USERS[i];
        text[n++] = '<option value="' + user.userid + '">' + user.username + '</option>';
    }
    $('#user_filter').html(text.join(''));
    getUpdate();
}   

function updateTable(serverhealths) {
    for (var servername in serverhealths) {
        var health = serverhealths[servername];
        var cell = $("#" + servername);

        if (cell != null) {
            cell.removeClass("ok warn critical");
            cell.addClass(health2class(health));
            cell.find("a").html(health);
        }
    }
    window.setTimeout(getUpdate, 1000);
}

function health2class(state) { 
    return HEALTH2CLASS[state];
}

function getUpdate() {
    $.getJSON("serverhealth.json", {}, updateTable);
    var params = {
        "user_filter" : $('#user_filter').val(),
        "server_filter" : $('#server_filter').val(),
        "limit" : 21,
        "offset" : $('#log_offset').val()
    };
    $.getJSON("controlstate.json", params, showRequestState);
}

function permitted(action, state) {
    if (action == "start") {
        if (state == "DOWN" || state == "HUH?" || state == "UNKNOWN") return true
    } else if (action == "stop") {
        if (state == "RUNNING" || state == "ADMIN" || state == "OK") return true
    } else if (action == "kill") {
        return true
    } else if (action == "restart") {
        return true
    } else if (action == "stop nm") {
        return true
    } else if (action == "start nm") {
        return true
    }
    return false;
}

function submitReq(action) {
   var query = "action=" + action;
   var errors = [];
   var servers = [];
   
   $("#servers_table tbody :checked").each(function() {
       var a = this.nextSibling;
       var state = a.innerHTML;
       if (permitted(action, state)) {
           servers.push("&server=" + this.name);
       } else {
           errors.push("Cannot " + action + " server " + this.name  + ", server has state " + state);
       }    
   });

   if (errors.length > 0) {
       alert(errors.join("\n"));
   }

   if (servers.length > 0) {
       query += servers.join("");
       $.post("serversrequest.taak", query, function(data) {
           getUpdate();
       });
   }
   clearchk();
}

function showRequestState(orders) {
    var text = [];
    var odd = 0;
    var offset = parseInt($('#log_offset').val());
    var count = orders.length;
    if (offset > 0) {
        $('#prev').fadeIn('fast');
    } else {
        $('#prev').fadeOut('fast');
    }
    if (count > 20) {
        $('#next').fadeIn('fast');
    } else {
        $('#next').fadeOut('fast');
    }

    var n = Math.min(count, 20);
    for (i = 0; i < n; i++) {
        var order = orders[i];
        var username = order.username;
        var servername =  order.servername;
        var command = order.command;
        var status = order.status;
        var startdate = order.startdate;
        var enddate = '...';
        if (order.enddate != 'null'){
            enddate = order.enddate;
        }

        var row = "<tr" + (odd ? " class='odd'" : "") + ">" +
                  "<td>" + username + "</td><td>" + command + "</td><td>" +
                  servername + "</td><td>" + startdate + "</td><td>" +
                  enddate + "</td><td>" + status + "</td></tr>";
        text.push(row);
        odd = 1 - odd;
    }
    $('#logtable tbody').html(text.join(''));
}

function clearchk() {
    $('#servers_table input:checkbox').attr('checked', false);
}

function setup_env_checkbox(env, columns) {
   var th_id = env + '_checkbox';
   $('#' + th_id).bind('click', function() {
       var checked = $('#' + th_id).is(':checked');
       for (i = 0; i < columns.length; i++) {
           $('#servers_table tbody td:nth-child(' + columns[i] + ') input:checkbox').filter(':not([disabled])').attr('checked', checked);
       } 
   });	
}

function setup_column_checkbox(n) {
    var td = $('#servers_table thead tr.servertype td:nth-child(' + n + ')');
    $('input:checkbox', td).bind('click', function() {
       var checked = $('input:checkbox', td).is(':checked');
       $('#servers_table tbody td:nth-child(' + n + ') input:checkbox').filter(':not([disabled])').attr('checked', checked);
    });
}

function popup(obj) {
    var me = $(obj);
    var x = me.offset().left - $(document).scrollLeft() - 20;
    var y = me.offset().top - $(document).scrollTop() - 60;
    var id = me.parent().attr('id');
    var server = SERVERS[id];
    var domain = server.domain;
    var as = domain.adminserver;
    var admin_url = "http://" + as.address + ":" + as.port;
    var href = admin_url +  "/console";
    var pu = $('#popup');
    pu.dialog('open');
    pu.dialog('option', 'title', id);
    pu.dialog('option', 'position', [x, y]);
    $('#wlsconsole', pu).attr('href', href);
    $('#sbconsole', pu).remove();
    if (domain.domaintype == "alsb" || domain.domaintype == "osb") {
        var link = '<a id="sbconsole" target="_blank" href="' + admin_url + '/sbconsole">sbconsole</a>';
        $('#console_spacer', pu).after(link);
    }
    $('#address', pu).text(server.address)
    $('#port', pu).text('' + server.port)
    $('#jmxport', pu).text('' + server.jmxport)
    return false;
}

function ajaxErrorHandler(event, request, opts, error) {
    var p = $('#errorpopup');
    p.dialog('open');
}

function pager(dir) {
    var offset = parseInt($('#log_offset').val());
    if (dir == 'prev') {
        offset = offset < 20 ? 0 : offset - 20;
    } if (dir == 'next') {
        offset = offset + 20;
    }
    $('#log_offset').val("" + offset);
    $('#logtable tbody').html('');
    $('#prev').fadeOut('fast');
    $('#next').fadeOut('fast');
}

$(document).ready(function() {
    $.ajaxSetup({ cache: false });
    $('#popup').dialog({
        autoOpen : false,
        width : 340
    });
    $('#errorpopup').dialog({
        title: 'Error',
        autoOpen: false,
        width: 340
    });
    $(document).ajaxError(ajaxErrorHandler);
    setup_env_checkbox("dev", [2,3,4,5,6,7]);
    setup_env_checkbox("test", [8,9,10,11,12,13]);
    setup_env_checkbox("acc", [14,15,16,17,18,19]);
    setup_env_checkbox("prd", [20,21,22,23,24,25]); 
    for (var n = 1; n <= 25; n++) {
        setup_column_checkbox(n);
    }
    $.getJSON("config.json", {}, initTable);
    window.setInterval(function() {
        $.ajax({
            type: 'GET',
            url: '/doom/alive.txt',
            success: function(data, status, request) {
                if (jQuery.trim(data) != 'alive') {
                    $('#errorpopup').dialog('open');
                } else {
                    $('#errorpopup').dialog('close');
                }
            }
        });
    }, 4000);
    $('#prev').fadeOut('fast');
    $('#next').fadeOut('fast');
    $('#user_filter').change(function() {
        $('#server_filter').val('');
        $('#log_offset').val('0');
        $('#logtable tbody').html('');
    });
    $('#server_filter').keyup(function() {
        $('#user_filter').val('');
        $('#log_offset').val('0');
        $('#logtable tbody').html('');
    });
});
