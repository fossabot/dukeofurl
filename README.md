# Duke of Url
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2Fcharlvanniekerk%2Fdukeofurl.svg?type=shield)](https://app.fossa.io/projects/git%2Bgithub.com%2Fcharlvanniekerk%2Fdukeofurl?ref=badge_shield)


A simple IRC bot to fetch a resource from a URL posted to a channel and respond with some metadata about it.

For most URLs only the HTML page title will be posted but special support has been added for certain content types in order to include additional information.

It is designed to run inside an OCI container.

```bash
curl -o ~/dukeofurl.properties https://raw.githubusercontent.com/charlvanniekerk/dukeofurl/master/example.properties
docker run -d -u 1234:1234 -v ~/dukeofurl.properties:/dukeofurl.properties:ro --restart=unless-stopped charlvanniekerk/dukeofurl
```


## License
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2Fcharlvanniekerk%2Fdukeofurl.svg?type=large)](https://app.fossa.io/projects/git%2Bgithub.com%2Fcharlvanniekerk%2Fdukeofurl?ref=badge_large)