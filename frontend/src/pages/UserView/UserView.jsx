import SearchInterface from "../../shared/SearchInterface.jsx";
import SearchResult from "../../shared/SearchResult.jsx";
import "../../styles/styles.css";

export default function Search() {
    return (
        <SearchInterface
            ResultComponent={SearchResult}
        />
    );
}