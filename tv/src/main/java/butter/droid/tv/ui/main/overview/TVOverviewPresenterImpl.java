/*
 * This file is part of Butter.
 *
 * Butter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Butter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Butter. If not, see <http://www.gnu.org/licenses/>.
 */

package butter.droid.tv.ui.main.overview;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import butter.droid.base.PlayerTestConstants;
import butter.droid.base.manager.internal.provider.ProviderManager;
import butter.droid.base.manager.internal.youtube.YouTubeManager;
import butter.droid.provider.base.filter.Filter;
import butter.droid.provider.base.module.ItemsWrapper;
import butter.droid.provider.base.module.Media;
import butter.droid.provider.base.nav.NavItem;
import butter.droid.provider.filter.Pager;
import butter.droid.tv.R;
import butter.droid.tv.presenters.MediaCardPresenter.MediaCardItem;
import butter.droid.tv.presenters.MorePresenter.MoreItem;
import io.reactivex.MaybeObserver;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;

public class TVOverviewPresenterImpl implements TVOverviewPresenter {

    private final TVOverviewView view;
    private final ProviderManager providerManager;
    private final YouTubeManager youTubeManager;

    private int selectedRow = 0;

    @NonNull private final SparseArray<Disposable> listRequests = new SparseArray<>();
    @NonNull private final SparseArray<Disposable> sortersRequests = new SparseArray<>();

    public TVOverviewPresenterImpl(final TVOverviewView view, final ProviderManager providerManager, final YouTubeManager youTubeManager) {
        this.view = view;
        this.providerManager = providerManager;
        this.youTubeManager = youTubeManager;
    }

    @Override public void onActivityCreated() {

        view.setupProviderRows(providerManager.getProviderCount());
        view.setupMoreRow();

        loadProvidersData();
    }

    @Override public void rowSelected(final int index, @Nullable final Media mediaItem) {
        if (selectedRow != index) {
            selectedRow = index;
        }

        if (mediaItem != null) {
            view.updateBackgroundImage(mediaItem.getBackdrop());
        }
    }

    @Override public void moreItemClicked(final MoreItem item) {
        switch (item.getId()) {
            case R.id.more_item_settings:
                view.openPreferencesScreen();
                break;
            case R.id.more_item_filter:
                //noinspection ConstantConditions
                view.openMediaActivity(item.getTitle(), item.getProviderId(), new Filter(null, item.getSorter()));
                break;
            case R.id.more_player_tests:
                view.openTestPlayerPicker();
                break;
            default:
                throw new IllegalStateException("Unknown item id");
        }
    }

    @Override public void debugVideoSelected(final int index) {

        final String location = PlayerTestConstants.FILES[index];

        // TODO
        /*
        if (location.equals("dialog")) {
            view.showCustomDebugUrl();
        } else if (youTubeManager.isYouTubeUrl(location)) {
            Movie movie = new Movie(PlayerTestConstants.FILE_TYPES[index]);
            view.startTrailerScreen(movie, location);
        } else {
            final Movie media = new Movie();
            media.videoId = "bigbucksbunny";
            media.title = PlayerTestConstants.FILE_TYPES[index];
            media.subtitles = new HashMap<>();
            media.subtitles.put("en", "http://sv244.cf/bbb-subs.srt");

            providerManager.getCurrentSubsProvider().download(media, "en", new Callback() {
                @Override public void onFailure(Call call, IOException ex) {
                    view.startPlayerActivity(new StreamInfo(media, null, null, null, null, location));
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    view.startPlayerActivity(new StreamInfo(media, null, null, null, null, location));
                }
            });
        }
        */
    }

    @Override public void onDestroy() {
        for (int i = 0; i < listRequests.size(); i++) {
            cancelMovieCall(listRequests.keyAt(i));
        }
        for (int i = 0; i < sortersRequests.size(); i++) {
            cancelMovieSortersCall(sortersRequests.keyAt(i));
        }
    }

    private void loadProvidersData() {
        for (int i = 0; i < providerManager.getProviderCount(); i++) {
            loadProviderData(i);
        }
    }

    private void loadProviderData(final int providerId) {
        loadProviderMedia(providerId);
        loadProviderSorters(providerId);
    }

    private void loadProviderMedia(final int providerId) {
        cancelMovieCall(providerId);
        final butter.droid.provider.MediaProvider provider = providerManager.getProvider(providerId);
        provider.getDefaultSorter()
                .flatMap(sorter -> {
                    Filter f;
                    if (sorter.isPresent()) {
                        f = new Filter(null, sorter.get());
                    } else {
                        f = new Filter(null, null);
                    }
                    return provider.items(f, new Pager(null));
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<ItemsWrapper>() {
                    @Override public void onSubscribe(final Disposable d) {
                        listRequests.append(providerId, d);
                    }

                    @Override public void onSuccess(final ItemsWrapper items) {
                        List<Media> mediaItems = items.getMedia();
                        List<MediaCardItem> cardItems = convertMediaToOverview(providerId, mediaItems);
                        view.displayProviderData(providerId, cardItems);

                        if (selectedRow == 0) {
                            view.updateBackgroundImage(mediaItems.get(0).getBackdrop());
                        }
                    }

                    @Override public void onError(final Throwable e) {
                        view.showErrorMessage(R.string.movies_error);
                    }
                });

    }

    private void loadProviderSorters(final int providerId) {
        cancelMovieSortersCall(providerId);
        providerManager.getProvider(providerId).navigation()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new MaybeObserver<List<NavItem>>() {
                    @Override public void onSubscribe(final Disposable d) {
                        sortersRequests.put(providerId, d);
                    }

                    @Override public void onSuccess(final List<NavItem> value) {
                        view.displayProviderSorters(providerId, value);
                    }

                    @Override public void onError(final Throwable e) {
                        // fail quietly
                    }

                    @Override public void onComplete() {
                        // nothing to do
                    }
                });
    }

    private void cancelMovieCall(int providerId) {
        Disposable d = listRequests.get(providerId);
        if (d != null) {
            d.dispose();
            listRequests.remove(providerId);
        }
    }

    private void cancelMovieSortersCall(int providerId) {
        Disposable d = sortersRequests.get(providerId);
        if (d != null) {
            d.dispose();
            sortersRequests.remove(providerId);
        }
    }

    public static List<MediaCardItem> convertMediaToOverview(final int providerId, final List<Media> items) {
        List<MediaCardItem> list = new ArrayList<>();
        for (Media media : items) {
            list.add(new MediaCardItem(providerId, media));
        }
        return list;
    }

}