function getOrderNoPrefix(tenant, owner, jobDefinitions, jobParameters) {
    return formatDate(new Date());
}

function formatDate(date) {
  var year = date.getFullYear();
  var month = addZero(date.getMonth() + 1);
  var day = addZero(date.getDate());
  return year + '' + month + '' + day;
}

function addZero(num) {
  return num < 10 ? ('0' + num) : ('' + num);
}

getOrderNoPrefix(args.tenant, args.owner, args.jobDefinitions, args.jobParameters);