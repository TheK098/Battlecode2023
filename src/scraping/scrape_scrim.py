# based on my old code from https://github.com/skittles1412/Battlecode2021/blob/master/src/scraping/scrape_scrim.py


from collections import defaultdict
from time import sleep

from selenium import webdriver
from selenium.common.exceptions import NoSuchElementException, StaleElementReferenceException
from selenium.webdriver.common.by import By

CREDENTIALS = open("credentials.txt", "r").readlines()
USERNAME = CREDENTIALS[0].strip()
PASSWORD = CREDENTIALS[1].strip()
PAGES = 11

BASE_URL = "https://play.battlecode.org/bc23"
TEAM_NAME = "amogus"


class DashboardScraper:
    def __init__(self):
        self.browser = webdriver.Chrome()

    def run(self):
        self.browser.get(f"https://play.battlecode.org/login")
        self.login()
        while self.browser.current_url.endswith("login"):
            sleep(0.1)

        self.browser.get(f"{BASE_URL}/scrimmaging")

        result = []
        links = []
        for page in range(1, PAGES + 1):
            for attempt in range(20):
                sleep(0.5)
                try:
                    self.browser.find_element(By.CSS_SELECTOR, f'div.col-md-12 > div.card + div.pagination-control li:nth-child({page + 1}) a.page-link').click()
                    scrims = self.scrape()
                    result.extend(scrims[0])
                    links.extend(scrims[1])
                    if len(scrims[0]) > 0:
                        break
                    else:  # scrims may still be loading, wait a bit
                        print(f"Page {page} has no finished scrims found, waiting")
                except (NoSuchElementException, StaleElementReferenceException) as e:
                    # print(e)
                    pass

        self.browser.quit()

        return result[::-1], links[::-1]

    def login(self):
        username_box = self.browser.find_element(By.ID, "username")
        password_box = self.browser.find_element(By.ID, "password")
        username_box.send_keys(USERNAME)
        password_box.send_keys(PASSWORD)
        self.browser.find_element(By.CSS_SELECTOR, 'button[type="submit"]').click()

    def scrape(self):
        table_body = self.browser.find_element(By.CSS_SELECTOR, "div.col-md-12 > div.card > div > table > tbody")
        rows = table_body.find_elements(By.CSS_SELECTOR, "tr")

        scrims = []
        links = []
        for row in rows:
            cols = row.find_elements(By.CSS_SELECTOR, "td")
            assert len(cols) == 7

            time = cols[6].text
            if cols[4].text in ("Rejected", "Error", "Pending"):
                continue
            score = cols[0].text
            opp = cols[2].text
            linked_time = time
            links.append("N/A")
            if cols[5].text != "N/A":
                links[-1] = cols[5].find_element(By.CSS_SELECTOR, 'a').get_attribute("href")
                linked_time = "EQUALSHYPERLINK(\"" + links[-1] + "\"COMMA\"" + time.replace(',', '') + "\")"

            elems = [linked_time, opp, score if len(score) > 3 else "N/A"]
            scrims.append(','.join(elems))
        return scrims, links


class MatchScraper:
    def __init__(self, browser, link):
        self.browser = browser
        self.browser.get(link)

    def exists(self, by: By, selector: str) -> bool:
        try:
            self.browser.find_element(by, selector)
            return True
        except NoSuchElementException:
            return False

    def run(self):
        while not self.exists(By.CSS_SELECTOR, "div.gameWrapper"):
            self.browser.find_element(By.XPATH, '//button[contains(@class, "modebutton")][text()="Queue"]').click()
            sleep(0.1)

        teams = self.browser.find_element(By.CSS_SELECTOR, "span.red").text.strip(), self.browser.find_element(
            By.CSS_SELECTOR, "span.blue").text.strip()
        opponent = teams[0] if teams[0] != TEAM_NAME else teams[1]

        maps = []
        durations = []
        winners = []
        for i in range(3):
            # nth-of-type is 1-indexed
            info = self.browser.find_element(By.CSS_SELECTOR, f"div.gameWrapper > div:nth-of-type({i + 1})").text
            round_map = info.split('-')[0]
            duration = info.strip().split()[-2]
            maps.append(round_map.strip())
            durations.append(duration.strip())
            winners.append(info.split('-', maxsplit=1)[1].rsplit(" wins ", maxsplit=1)[0].strip())

        return opponent, maps, durations, winners


def main():
    scrims, links = DashboardScraper().run()
    # print(scrims, links)
    browser = webdriver.Chrome()

    output = []
    team_record = defaultdict(lambda: [0, 0])
    map_record = defaultdict(lambda: [0, 0])
    for scrim, link in zip(scrims, links):
        if link.startswith("https://"):
            opponent, maps, durations, winners = MatchScraper(browser, link).run()
            for m, d, w in zip(maps, durations, winners):
                idx = w != TEAM_NAME
                team_record[opponent][idx] += 1
                map_record[m][idx] += 1
        else:
            maps, durations, winners = [["N/A"] * 3] * 3
        output.append(scrim)
        for i in range(3):
            output.append(f"Round {i + 1},{maps[i]},{durations[i]},{winners[i]}")
        output.append('')

    browser.quit()

    print('\n'.join(output))
    print("\n\n")
    for team, record in team_record.items():
        print(f"{team},{record[0]},{record[1]}")
    for m, record in map_record.items():
        print(f"{m},{record[0]},{record[1]}")


if __name__ == '__main__':
    main()
